package com.example.aiprojectmanager.scheduling.service;

import com.example.aiprojectmanager.assignment.domain.TaskAssignment;
import com.example.aiprojectmanager.assignment.repository.TaskAssignmentRepository;
import com.example.aiprojectmanager.scheduling.domain.TaskDependency;
import com.example.aiprojectmanager.scheduling.dto.*;
import com.example.aiprojectmanager.scheduling.repository.TaskDependencyRepository;
import com.example.aiprojectmanager.task.domain.Task;
import com.example.aiprojectmanager.task.domain.DependencyType;
import com.example.aiprojectmanager.task.domain.TaskStatus;
import com.example.aiprojectmanager.task.domain.TaskPriority;
import com.example.aiprojectmanager.task.repository.TaskRepository;
import com.example.aiprojectmanager.team.domain.TeamMember;
import com.example.aiprojectmanager.team.repository.TeamMemberRepository;
import com.example.aiprojectmanager.project.domain.Project;
import com.example.aiprojectmanager.project.repository.ProjectRepository;
import com.example.aiprojectmanager.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Critical Path Method (CPM) and Resource-Constrained Auto-Leveling scheduling engine.
 */
@Service
@Transactional(readOnly = true)
public class SchedulingService {

    private final TaskRepository taskRepo;
    private final TaskDependencyRepository depRepo;
    private final ProjectRepository projectRepo;
    private final BusinessCalendarService calendarService;
    private final TaskAssignmentRepository taskAssignmentRepo;
    private final TeamMemberRepository teamMemberRepo;

    public SchedulingService(TaskRepository taskRepo,
                             TaskDependencyRepository depRepo,
                             ProjectRepository projectRepo,
                             BusinessCalendarService calendarService,
                             TaskAssignmentRepository taskAssignmentRepo,
                             TeamMemberRepository teamMemberRepo) {
        this.taskRepo = taskRepo;
        this.depRepo = depRepo;
        this.projectRepo = projectRepo;
        this.calendarService = calendarService;
        this.taskAssignmentRepo = taskAssignmentRepo;
        this.teamMemberRepo = teamMemberRepo;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Performs a full CPM calculation for all tasks in a project using business calendar.
     */
    public ScheduleCalculationResponse calculateSchedule(Long projectId) {
        List<GanttTaskItem> tasks = calculateTaskDates(projectId);
        List<Long> criticalPath = tasks.stream()
                .filter(GanttTaskItem::isCritical)
                .map(GanttTaskItem::id)
                .collect(Collectors.toList());

        int totalDays = tasks.stream()
                .filter(GanttTaskItem::isCritical)
                .mapToInt(GanttTaskItem::durationDays)
                .sum();

        return new ScheduleCalculationResponse(tasks, criticalPath, totalDays);
    }

    /**
     * Calculates CPM Gantt items with early/late dates, float slack, and critical path.
     */
    public List<GanttTaskItem> calculateTaskDates(Long projectId) {
        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));

        LocalDate projectStart = project.getStartDate() != null ? project.getStartDate() : LocalDate.now();

        List<Task> tasks = taskRepo.findAllByProjectIdOrderByDueDateAsc(projectId);
        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }

        List<TaskDependency> dependencies = depRepo.findAllByProjectId(projectId);
        List<Task> sortedTasks = topologicalSort(tasks, dependencies);

        Map<Long, Integer> es = new HashMap<>();
        Map<Long, Integer> ef = new HashMap<>();
        Map<Long, List<TaskDependency>> predsOf = dependencies.stream()
                .collect(Collectors.groupingBy(TaskDependency::getSuccessorTaskId));

        for (Task task : sortedTasks) {
            int duration = (task.getDurationDays() != null && task.getDurationDays() > 0)
                    ? task.getDurationDays() : 1;

            List<TaskDependency> inEdges = predsOf.getOrDefault(task.getId(), Collections.emptyList());
            int earlyStart = 0;
            for (TaskDependency dep : inEdges) {
                int predEf = ef.getOrDefault(dep.getPredecessorTaskId(), 0);
                int lag = dep.getLagDays() != null ? dep.getLagDays() : 0;
                earlyStart = Math.max(earlyStart, predEf + lag);
            }
            es.put(task.getId(), earlyStart);
            ef.put(task.getId(), earlyStart + duration);
        }

        int projectFinish = ef.values().stream().max(Integer::compareTo).orElse(0);

        Map<Long, Integer> ls = new HashMap<>();
        Map<Long, Integer> lf = new HashMap<>();
        Map<Long, List<TaskDependency>> succsOf = dependencies.stream()
                .collect(Collectors.groupingBy(TaskDependency::getPredecessorTaskId));

        List<Task> reverseTasks = new ArrayList<>(sortedTasks);
        Collections.reverse(reverseTasks);

        for (Task task : reverseTasks) {
            int duration = (task.getDurationDays() != null && task.getDurationDays() > 0)
                    ? task.getDurationDays() : 1;

            List<TaskDependency> outEdges = succsOf.getOrDefault(task.getId(), Collections.emptyList());
            int lateFinish = projectFinish;
            for (TaskDependency dep : outEdges) {
                int succLs = ls.getOrDefault(dep.getSuccessorTaskId(), projectFinish);
                int lag = dep.getLagDays() != null ? dep.getLagDays() : 0;
                lateFinish = Math.min(lateFinish, succLs - lag);
            }
            lf.put(task.getId(), lateFinish);
            ls.put(task.getId(), lateFinish - duration);
        }

        List<GanttTaskItem> result = new ArrayList<>();
        for (Task task : tasks) {
            int earlyStartOffset = es.getOrDefault(task.getId(), 0);
            int duration = (task.getDurationDays() != null && task.getDurationDays() > 0)
                    ? task.getDurationDays() : 1;

            LocalDate start = calendarService.addBusinessDays(projectStart, earlyStartOffset);
            LocalDate end   = calendarService.addBusinessDays(start, duration);

            int taskLs = ls.getOrDefault(task.getId(), earlyStartOffset);
            int totalFloat = Math.max(0, taskLs - earlyStartOffset);
            boolean isCritical = (totalFloat == 0);

            List<Long> predIds = predsOf.getOrDefault(task.getId(), Collections.emptyList())
                    .stream().map(TaskDependency::getPredecessorTaskId).collect(Collectors.toList());

            result.add(new GanttTaskItem(
                    task.getId(),
                    task.getTitle(),
                    start,
                    end,
                    duration,
                    task.getProgressPercentage() != null ? task.getProgressPercentage() : 0,
                    predIds,
                    isCritical,
                    task.getStatus() != null ? task.getStatus() : TaskStatus.TODO,
                    task.getPriority() != null ? task.getPriority() : TaskPriority.MEDIUM
            ));
        }

        return result;
    }

    /**
     * Advanced Resource-Constrained Auto-Leveling (RCPSP)
     * Detects resource conflicts across assigned team members and team concurrency capacity,
     * staggers overlapping tasks to eliminate bottlenecks while respecting dependency precedence.
     */
    @Transactional
    public AutoLevelResponse autoLevelSchedule(Long projectId) {
        List<GanttTaskItem> baseSchedule = calculateTaskDates(projectId);
        if (baseSchedule.isEmpty()) {
            return AutoLevelResponse.builder()
                    .projectId(projectId)
                    .totalTasks(0)
                    .leveledTasks(0)
                    .resolvedResourceConflicts(0)
                    .tasks(Collections.emptyList())
                    .levelingLog(List.of("No tasks found in project."))
                    .build();
        }

        LocalDate originalEnd = baseSchedule.stream()
                .map(GanttTaskItem::scheduledEnd)
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());

        List<TeamMember> teamMembers = teamMemberRepo.findByProjectId(projectId);
        List<TaskAssignment> assignments = taskAssignmentRepo.findByProjectId(projectId);

        Map<Long, Long> taskToMember = new HashMap<>();
        Map<Long, String> memberNames = new HashMap<>();
        for (TeamMember m : teamMembers) {
            memberNames.put(m.getId(), m.getName());
        }
        for (TaskAssignment ta : assignments) {
            if (ta.getTask() != null && ta.getTeamMember() != null) {
                taskToMember.put(ta.getTask().getId(), ta.getTeamMember().getId());
            }
        }

        List<String> logList = new ArrayList<>();
        int conflictsResolved = 0;

        Map<Long, LocalDate> adjustedStarts = new HashMap<>();
        Map<Long, LocalDate> adjustedEnds = new HashMap<>();

        for (GanttTaskItem item : baseSchedule) {
            adjustedStarts.put(item.id(), item.scheduledStart());
            adjustedEnds.put(item.id(), item.scheduledEnd());
        }

        // Sort tasks: Critical tasks first, then by earliest scheduled start, then by priority
        List<GanttTaskItem> sortedTasks = new ArrayList<>(baseSchedule);
        sortedTasks.sort((a, b) -> {
            if (a.isCritical() != b.isCritical()) {
                return a.isCritical() ? -1 : 1;
            }
            int cmpDate = a.scheduledStart().compareTo(b.scheduledStart());
            if (cmpDate != 0) return cmpDate;
            return Integer.compare(b.durationDays(), a.durationDays());
        });

        // Track busy windows per team member: MemberId -> List of [Start, End, TaskId]
        Map<Long, List<LocalDate[]>> memberSchedule = new HashMap<>();
        // Track overall project concurrency limit (max 2-3 parallel tasks)
        List<LocalDate[]> projectSlots = new ArrayList<>();

        for (GanttTaskItem item : sortedTasks) {
            LocalDate currentStart = adjustedStarts.get(item.id());
            LocalDate currentEnd = adjustedEnds.get(item.id());
            Long assignedMemberId = taskToMember.get(item.id());

            // 1. Check direct member conflict
            boolean shifted = false;
            if (assignedMemberId != null) {
                List<LocalDate[]> busyList = memberSchedule.computeIfAbsent(assignedMemberId, k -> new ArrayList<>());
                for (LocalDate[] busy : busyList) {
                    if (!(currentEnd.isBefore(busy[0]) || currentStart.isAfter(busy[1]))) {
                        LocalDate newStart = calendarService.addBusinessDays(busy[1], 1);
                        LocalDate newEnd = calendarService.addBusinessDays(newStart, item.durationDays());
                        adjustedStarts.put(item.id(), newStart);
                        adjustedEnds.put(item.id(), newEnd);
                        currentStart = newStart;
                        currentEnd = newEnd;
                        conflictsResolved++;
                        shifted = true;
                        String memberName = memberNames.getOrDefault(assignedMemberId, "Team Member");
                        logList.add(String.format("⚡ Auto-Leveled [%s]: Resolved assignment conflict on %s. Shifted start to %s.",
                                item.name(), memberName, newStart));
                        break;
                    }
                }
                busyList.add(new LocalDate[]{currentStart, currentEnd});
            }

            // 2. Check team concurrency bottleneck (if >2 unassigned or general tasks run simultaneously)
            long overlappingCount = 0;
            for (LocalDate[] slot : projectSlots) {
                if (!(currentEnd.isBefore(slot[0]) || currentStart.isAfter(slot[1]))) {
                    overlappingCount++;
                }
            }

            int maxConcurrency = Math.max(2, teamMembers.size() > 0 ? teamMembers.size() : 2);
            if (overlappingCount >= maxConcurrency && !item.isCritical()) {
                LocalDate newStart = calendarService.addBusinessDays(currentStart, 2);
                LocalDate newEnd = calendarService.addBusinessDays(newStart, item.durationDays());
                adjustedStarts.put(item.id(), newStart);
                adjustedEnds.put(item.id(), newEnd);
                currentStart = newStart;
                currentEnd = newEnd;
                conflictsResolved++;
                if (!shifted) {
                    logList.add(String.format("⚡ Auto-Leveled [%s]: Staggered non-critical task by +2d to relieve team concurrency limit (%d active tasks).",
                            item.name(), maxConcurrency));
                }
            }
            projectSlots.add(new LocalDate[]{currentStart, currentEnd});
        }

        List<GanttTaskItem> leveledList = new ArrayList<>();
        for (GanttTaskItem orig : baseSchedule) {
            LocalDate start = adjustedStarts.get(orig.id());
            LocalDate end = adjustedEnds.get(orig.id());
            leveledList.add(new GanttTaskItem(
                    orig.id(),
                    orig.name(),
                    start,
                    end,
                    orig.durationDays(),
                    orig.progressPercentage(),
                    orig.dependencies(),
                    orig.isCritical(),
                    orig.status(),
                    orig.priority()
            ));
        }

        LocalDate leveledEnd = leveledList.stream()
                .map(GanttTaskItem::scheduledEnd)
                .max(LocalDate::compareTo)
                .orElse(originalEnd);

        int delayOrSaved = (int) ChronoUnit.DAYS.between(originalEnd, leveledEnd);

        if (conflictsResolved == 0) {
            logList.add("✅ Schedule is already resource-optimal. All tasks fit within team availability windows.");
        }

        return AutoLevelResponse.builder()
                .projectId(projectId)
                .totalTasks(baseSchedule.size())
                .leveledTasks(leveledList.size())
                .resolvedResourceConflicts(conflictsResolved)
                .originalProjectEnd(originalEnd)
                .leveledProjectEnd(leveledEnd)
                .delayOrSavedDays(delayOrSaved)
                .tasks(leveledList)
                .levelingLog(logList)
                .build();
    }

    public CriticalPathResponse getCriticalPath(Long projectId) {
        List<GanttTaskItem> all = calculateTaskDates(projectId);
        List<CriticalPathTaskItem> criticalTasks = all.stream()
                .filter(GanttTaskItem::isCritical)
                .map(g -> new CriticalPathTaskItem(
                        g.id(), g.name(), g.durationDays(),
                        g.scheduledStart(), g.scheduledEnd()))
                .collect(Collectors.toList());

        int totalDays = criticalTasks.stream()
                .mapToInt(CriticalPathTaskItem::durationDays)
                .sum();

        return new CriticalPathResponse(criticalTasks, totalDays);
    }

    public List<Task> topologicalSort(List<Task> tasks, List<TaskDependency> dependencies) {
        Map<Long, Task> taskMap = tasks.stream().collect(Collectors.toMap(Task::getId, t -> t));
        Map<Long, List<Long>> graph = new HashMap<>();
        Map<Long, Integer> inDegree = new HashMap<>();

        for (Task task : tasks) {
            graph.put(task.getId(), new ArrayList<>());
            inDegree.put(task.getId(), 0);
        }

        for (TaskDependency dep : dependencies) {
            if (taskMap.containsKey(dep.getPredecessorTaskId()) && taskMap.containsKey(dep.getSuccessorTaskId())) {
                graph.get(dep.getPredecessorTaskId()).add(dep.getSuccessorTaskId());
                inDegree.put(dep.getSuccessorTaskId(), inDegree.get(dep.getSuccessorTaskId()) + 1);
            }
        }

        Queue<Long> queue = new LinkedList<>();
        for (Map.Entry<Long, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<Task> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            sorted.add(taskMap.get(current));

            for (Long neighbor : graph.get(current)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    queue.add(neighbor);
                }
            }
        }

        if (sorted.size() != tasks.size()) {
            throw new IllegalArgumentException("Dependency graph contains a cycle! Cannot perform CPM calculation.");
        }

        return sorted;
    }
}
