package com.example.aiprojectmanager.scheduling.service;

import com.example.aiprojectmanager.scheduling.domain.TaskDependency;
import com.example.aiprojectmanager.scheduling.dto.*;
import com.example.aiprojectmanager.scheduling.repository.TaskDependencyRepository;
import com.example.aiprojectmanager.task.domain.DependencyType;
import com.example.aiprojectmanager.task.domain.Task;
import com.example.aiprojectmanager.task.repository.TaskRepository;
import com.example.aiprojectmanager.common.ForbiddenException;
import com.example.aiprojectmanager.common.NotFoundException;
import com.example.aiprojectmanager.project.repository.ProjectRepository;
import com.example.aiprojectmanager.auth.CurrentUserService;
import com.example.aiprojectmanager.project.domain.Project;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Orchestrates dependency management and Gantt data assembly.
 * Delegates CPM calculation to {@link SchedulingService}.
 */
@Service
@Transactional
public class GanttDataService {

    private final SchedulingService schedulingService;
    private final TaskDependencyRepository depRepo;
    private final TaskRepository taskRepo;
    private final ProjectRepository projectRepo;
    private final CurrentUserService currentUserService;

    public GanttDataService(SchedulingService schedulingService,
                            TaskDependencyRepository depRepo,
                            TaskRepository taskRepo,
                            ProjectRepository projectRepo,
                            CurrentUserService currentUserService) {
        this.schedulingService = schedulingService;
        this.depRepo = depRepo;
        this.taskRepo = taskRepo;
        this.projectRepo = projectRepo;
        this.currentUserService = currentUserService;
    }

    // ── Dependency CRUD ───────────────────────────────────────────────────────

    /** Creates a dependency edge after validating ownership and constraints. */
    public DependencyResponse createDependency(Long projectId, Long successorTaskId,
                                               CreateDependencyRequest req) {
        Project project = ownedProject(projectId);

        Long predecessorTaskId = req.predecessorTaskId();

        // Self-dependency guard
        if (predecessorTaskId.equals(successorTaskId)) {
            throw new IllegalArgumentException("A task cannot depend on itself.");
        }

        // Both tasks must exist in this project
        Task predecessor = taskInProject(predecessorTaskId, projectId);
        Task successor   = taskInProject(successorTaskId,   projectId);

        // Duplicate edge guard
        if (depRepo.findByPredecessorTaskIdAndSuccessorTaskId(predecessorTaskId, successorTaskId).isPresent()) {
            throw new IllegalArgumentException(
                    "Dependency between task " + predecessorTaskId + " and task " + successorTaskId + " already exists.");
        }

        TaskDependency dep = new TaskDependency();
        dep.setProjectId(projectId);
        dep.setPredecessorTaskId(predecessorTaskId);
        dep.setSuccessorTaskId(successorTaskId);
        dep.setDependencyType(req.dependencyType() != null ? req.dependencyType() : DependencyType.FINISH_TO_START);
        dep.setLagDays(req.lagDays() != null ? req.lagDays() : 0);

        // Circular dependency check BEFORE persisting
        List<TaskDependency> existing = depRepo.findAllByProjectId(projectId);
        existing.add(dep); // tentative addition
        List<Task> tasks = taskRepo.findAllByProjectIdOrderByDueDateAsc(projectId);
        schedulingService.topologicalSort(tasks, existing); // throws if cycle

        TaskDependency saved = depRepo.save(dep);
        return toResponse(saved);
    }

    /** Lists all dependency edges for a project. */
    @Transactional(readOnly = true)
    public List<DependencyResponse> listDependencies(Long projectId) {
        ownedProject(projectId);
        return depRepo.findAllByProjectId(projectId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Deletes a dependency by ID, verifying project ownership. */
    public void deleteDependency(Long projectId, Long dependencyId) {
        ownedProject(projectId);
        TaskDependency dep = depRepo.findById(dependencyId)
                .orElseThrow(() -> new NotFoundException("Dependency not found: " + dependencyId));
        if (!dep.getProjectId().equals(projectId)) {
            throw new ForbiddenException("Dependency does not belong to this project.");
        }
        depRepo.deleteById(dependencyId);
    }

    // ── Schedule & Gantt ─────────────────────────────────────────────────────

    /** Recalculates dates and returns a full schedule + critical path. */
    public ScheduleCalculationResponse calculateSchedule(Long projectId) {
        ownedProject(projectId);
        List<GanttTaskItem> tasks = schedulingService.calculateTaskDates(projectId);
        List<Long> cp = tasks.stream()
                .filter(g -> g.isCritical())
                .map(g -> g.id())
                .collect(Collectors.toList());
        int totalDays = tasks.stream()
                .filter(g -> g.isCritical())
                .mapToInt(g -> g.durationDays())
                .sum();
        return new ScheduleCalculationResponse(tasks, cp, totalDays);
    }

    /** Returns Gantt chart data for a project. */
    @Transactional(readOnly = true)
    public GanttDataResponse getGanttData(Long projectId) {
        Project project = ownedProject(projectId);
        List<GanttTaskItem> tasks = schedulingService.calculateTaskDates(projectId);
        List<Long> criticalPath = tasks.stream()
                .filter(g -> g.isCritical())
                .map(g -> g.id())
                .collect(Collectors.toList());

        LocalDate projectStart = project.getStartDate() != null
                ? project.getStartDate() : LocalDate.now();
        LocalDate projectEnd = tasks.stream()
                .map(g -> g.scheduledEnd())
                .filter(d -> d != null)
                .max(LocalDate::compareTo)
                .orElse(project.getEndDate() != null ? project.getEndDate() : projectStart.plusDays(30));

        return new GanttDataResponse(tasks, projectStart, projectEnd, criticalPath);
    }

    /** Returns critical path data for a project. */
    @Transactional(readOnly = true)
    public CriticalPathResponse getCriticalPath(Long projectId) {
        ownedProject(projectId);
        return schedulingService.getCriticalPath(projectId);
    }

    /** Auto-levels the schedule to resolve resource congestion. */
    public AutoLevelResponse autoLevelSchedule(Long projectId) {
        ownedProject(projectId);
        return schedulingService.autoLevelSchedule(projectId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Project ownedProject(Long projectId) {
        Long userId = currentUserService.getCurrentUserId();
        Project p = projectRepo.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
        if (!p.getOwnerId().equals(userId)) {
            throw new ForbiddenException("You do not have access to this project.");
        }
        return p;
    }

    private Task taskInProject(Long taskId, Long projectId) {
        Task t = taskRepo.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found: " + taskId));
        if (!t.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException(
                    "Task " + taskId + " does not belong to project " + projectId);
        }
        return t;
    }

    private DependencyResponse toResponse(TaskDependency d) {
        return new DependencyResponse(
                d.getId(), d.getProjectId(),
                d.getPredecessorTaskId(), d.getSuccessorTaskId(),
                d.getDependencyType(), d.getLagDays());
    }
}
