package com.example.aiprojectmanager.scheduling.service;

import com.example.aiprojectmanager.scheduling.domain.TaskDependency;
import com.example.aiprojectmanager.scheduling.dto.*;
import com.example.aiprojectmanager.scheduling.repository.TaskDependencyRepository;
import com.example.aiprojectmanager.task.domain.Task;
import com.example.aiprojectmanager.task.domain.DependencyType;
import com.example.aiprojectmanager.task.repository.TaskRepository;
import com.example.aiprojectmanager.project.domain.Project;
import com.example.aiprojectmanager.project.repository.ProjectRepository;
import com.example.aiprojectmanager.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Critical Path Method (CPM) scheduling engine.
 *
 * <p>Algorithm overview:
 * <ol>
 *   <li>Topological sort (Kahn's algorithm) — detects cycles as a side-effect.</li>
 *   <li>Forward pass — compute Earliest Start (ES) and Earliest Finish (EF) for each task.</li>
 *   <li>Backward pass — compute Latest Start (LS) and Latest Finish (LF) for each task.</li>
 *   <li>Slack = LS − ES. Tasks with slack == 0 are on the critical path.</li>
 * </ol>
 *
 * <p>All dates are calendar days (no weekend/holiday awareness in Milestone 3).
 */
@Service
@Transactional(readOnly = true)
public class SchedulingService {

    private final TaskRepository taskRepo;
    private final TaskDependencyRepository depRepo;
    private final ProjectRepository projectRepo;
    private final BusinessCalendarService calendarService;

    public SchedulingService(TaskRepository taskRepo,
                             TaskDependencyRepository depRepo,
                             ProjectRepository projectRepo,
                             BusinessCalendarService calendarService) {
        this.taskRepo = taskRepo;
        this.depRepo = depRepo;
        this.projectRepo = projectRepo;
        this.calendarService = calendarService;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Performs a full CPM calculation for all tasks in a project.
     *
     * @return Gantt items with computed scheduledStart / scheduledEnd / isCritical.
     */
    public List<GanttTaskItem> calculateTaskDates(Long projectId) {
        Project project = getProject(projectId);
        List<Task> tasks = taskRepo.findAllByProjectIdOrderByDueDateAsc(projectId);
        List<TaskDependency> deps = depRepo.findAllByProjectId(projectId);

        if (tasks.isEmpty()) return Collections.emptyList();

        List<Task> ordered = topologicalSort(tasks, deps);
        LocalDate projectStart = project.getStartDate() != null
                ? project.getStartDate() : LocalDate.now();

        // ── Forward pass ──────────────────────────────────────────────────────
        // es[taskId] = Earliest Start (as days offset from projectStart)
        Map<Long, Integer> es = new HashMap<>();
        Map<Long, Integer> ef = new HashMap<>();

        // Build successor→predecessors lookup
        Map<Long, List<TaskDependency>> predsOf = deps.stream()
                .collect(Collectors.groupingBy(d -> d.getSuccessorTaskId()));

        for (Task t : ordered) {
            List<TaskDependency> inEdges = predsOf.getOrDefault(t.getId(), List.of());
            int startDay = 0;
            for (TaskDependency dep : inEdges) {
                int candidateStart = computeCandidateStart(dep, es, ef, t);
                startDay = Math.max(startDay, candidateStart);
            }
            int duration = t.getDurationDays() != null ? t.getDurationDays() : 1;
            es.put(t.getId(), startDay);
            ef.put(t.getId(), startDay + duration);
        }

        // ── Backward pass ─────────────────────────────────────────────────────
        // Project finish = max of all EF values
        int projectFinishDay = ef.values().stream().max(Integer::compareTo).orElse(0);

        Map<Long, Integer> ls = new HashMap<>();
        Map<Long, Integer> lf = new HashMap<>();

        // Build predecessor→successors lookup
        Map<Long, List<TaskDependency>> succsOf = deps.stream()
                .collect(Collectors.groupingBy(d -> d.getPredecessorTaskId()));

        List<Task> reversed = new ArrayList<>(ordered);
        Collections.reverse(reversed);

        for (Task t : reversed) {
            List<TaskDependency> outEdges = succsOf.getOrDefault(t.getId(), List.of());
            int finishDay = projectFinishDay;
            for (TaskDependency dep : outEdges) {
                int candidateFinish = computeCandidateFinish(dep, ls, lf, t);
                finishDay = Math.min(finishDay, candidateFinish);
            }
            int duration = t.getDurationDays() != null ? t.getDurationDays() : 1;
            lf.put(t.getId(), finishDay);
            ls.put(t.getId(), finishDay - duration);
        }

        // ── Build results ─────────────────────────────────────────────────────
        Map<Long, List<Long>> predecessorIds = deps.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getSuccessorTaskId(),
                        Collectors.mapping(d -> d.getPredecessorTaskId(), Collectors.toList())
                ));

        return ordered.stream().map(t -> {
            int esDay = es.get(t.getId());
            int lsDay = ls.get(t.getId());
            int slack = lsDay - esDay;
            // Use business-day-aware date placement
            LocalDate startDate = calendarService.addBusinessDays(projectStart, esDay);
            LocalDate endDate   = calendarService.addBusinessDays(projectStart, ef.get(t.getId()));
            return new GanttTaskItem(
                    t.getId(),
                    t.getTitle(),
                    startDate,
                    endDate,
                    t.getDurationDays() != null ? t.getDurationDays() : 1,
                    t.getProgressPercentage(),
                    predecessorIds.getOrDefault(t.getId(), List.of()),
                    slack == 0,
                    t.getStatus(),
                    t.getPriority()
            );
        }).collect(Collectors.toList());
    }

    /**
     * Returns the critical path (tasks with zero float/slack) in execution order.
     */
    public CriticalPathResponse getCriticalPath(Long projectId) {
        List<GanttTaskItem> all = calculateTaskDates(projectId);
        List<CriticalPathTaskItem> criticalTasks = all.stream()
                .filter(g -> g.isCritical())
                .map(g -> new CriticalPathTaskItem(
                        g.id(), g.name(), g.durationDays(),
                        g.scheduledStart(), g.scheduledEnd()))
                .collect(Collectors.toList());

        int totalDays = criticalTasks.stream()
                .mapToInt(c -> c.durationDays())
                .sum();

        return new CriticalPathResponse(criticalTasks, totalDays);
    }

    /**
     * Topological sort using Kahn's BFS algorithm.
     * Throws {@link IllegalArgumentException} if a circular dependency is detected.
     */
    public List<Task> topologicalSort(List<Task> tasks, List<TaskDependency> deps) {
        Map<Long, Task> taskMap = tasks.stream()
                .collect(Collectors.toMap(t -> t.getId(), t -> t));

        // in-degree count per task
        Map<Long, Integer> inDegree = new HashMap<>();
        for (Task t : tasks) inDegree.put(t.getId(), 0);

        // adjacency list: predecessor → list of successors
        Map<Long, List<Long>> adj = new HashMap<>();
        for (Task t : tasks) adj.put(t.getId(), new ArrayList<>());

        for (TaskDependency dep : deps) {
            Long pred = dep.getPredecessorTaskId();
            Long succ = dep.getSuccessorTaskId();
            if (taskMap.containsKey(pred) && taskMap.containsKey(succ)) {
                adj.get(pred).add(succ);
                inDegree.merge(succ, 1, Integer::sum);
            }
        }

        Queue<Long> queue = new LinkedList<>();
        for (Map.Entry<Long, Integer> e : inDegree.entrySet()) {
            if (e.getValue() == 0) queue.add(e.getKey());
        }

        List<Task> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            Long id = queue.poll();
            sorted.add(taskMap.get(id));
            for (Long succ : adj.get(id)) {
                int deg = inDegree.merge(succ, -1, Integer::sum);
                if (deg == 0) queue.add(succ);
            }
        }

        if (sorted.size() != tasks.size()) {
            throw new IllegalArgumentException(
                    "Circular dependency detected in project tasks. " +
                    "Please remove the cycle before calculating the schedule.");
        }
        return sorted;
    }

    /**
     * Validates that all dependency edges for a project are well-formed:
     * <ul>
     *   <li>No self-dependencies</li>
     *   <li>Predecessor and successor both belong to the same project</li>
     *   <li>No circular dependencies</li>
     * </ul>
     */
    public void validateDependencies(Long projectId) {
        List<Task> tasks = taskRepo.findAllByProjectIdOrderByDueDateAsc(projectId);
        List<TaskDependency> deps = depRepo.findAllByProjectId(projectId);
        Set<Long> taskIds = tasks.stream().map(t -> t.getId()).collect(Collectors.toSet());

        for (TaskDependency dep : deps) {
            if (dep.getPredecessorTaskId().equals(dep.getSuccessorTaskId())) {
                throw new IllegalArgumentException(
                        "Self-dependency detected on task " + dep.getPredecessorTaskId());
            }
            if (!taskIds.contains(dep.getPredecessorTaskId())) {
                throw new IllegalArgumentException(
                        "Predecessor task " + dep.getPredecessorTaskId() + " does not belong to project " + projectId);
            }
            if (!taskIds.contains(dep.getSuccessorTaskId())) {
                throw new IllegalArgumentException(
                        "Successor task " + dep.getSuccessorTaskId() + " does not belong to project " + projectId);
            }
        }
        // Will throw if cycle exists
        topologicalSort(tasks, deps);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Given a dependency edge and the current ES/EF maps, compute the earliest
     * day on which the successor can START, respecting the dependency type and lag.
     */
    private int computeCandidateStart(TaskDependency dep,
                                      Map<Long, Integer> es,
                                      Map<Long, Integer> ef,
                                      Task succ) {
        Long predId = dep.getPredecessorTaskId();
        int lag = dep.getLagDays() != null ? dep.getLagDays() : 0;
        int duration = succ.getDurationDays() != null ? succ.getDurationDays() : 1;
        return switch (dep.getDependencyType()) {
            case FINISH_TO_START  -> ef.getOrDefault(predId, 0) + lag;
            case START_TO_START   -> es.getOrDefault(predId, 0) + lag;
            case FINISH_TO_FINISH -> ef.getOrDefault(predId, 0) + lag - duration;
            case START_TO_FINISH  -> es.getOrDefault(predId, 0) + lag - duration;
        };
    }

    /**
     * Backward pass: compute the latest day the predecessor may FINISH
     * given a dependency edge and the LS/LF of the successor.
     */
    private int computeCandidateFinish(TaskDependency dep,
                                       Map<Long, Integer> ls,
                                       Map<Long, Integer> lf,
                                       Task pred) {
        Long succId = dep.getSuccessorTaskId();
        int lag = dep.getLagDays() != null ? dep.getLagDays() : 0;
        int duration = pred.getDurationDays() != null ? pred.getDurationDays() : 1;
        return switch (dep.getDependencyType()) {
            case FINISH_TO_START  -> ls.get(succId) - lag;
            case START_TO_START   -> ls.get(succId) - lag + duration;
            case FINISH_TO_FINISH -> lf.get(succId) - lag;
            case START_TO_FINISH  -> lf.get(succId) - lag + duration;
        };
    }

    private Project getProject(Long projectId) {
        return projectRepo.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
    }
}
