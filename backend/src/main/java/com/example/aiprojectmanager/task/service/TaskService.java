package com.example.aiprojectmanager.task.service;

import com.example.aiprojectmanager.auth.CurrentUserService;
import com.example.aiprojectmanager.common.*;
import com.example.aiprojectmanager.project.domain.Project;
import com.example.aiprojectmanager.project.repository.ProjectRepository;
import com.example.aiprojectmanager.task.domain.*;
import com.example.aiprojectmanager.task.dto.*;
import com.example.aiprojectmanager.task.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

import com.example.aiprojectmanager.scheduling.repository.TaskDependencyRepository;
import com.example.aiprojectmanager.assignment.repository.TaskAssignmentRepository;

@Service
@Transactional
public class TaskService {
    private final TaskRepository tasks;
    private final ProjectRepository projects;
    private final CurrentUserService currentUserService;
    private final TaskDependencyRepository taskDependencyRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;

    public TaskService(TaskRepository tasks, ProjectRepository projects, CurrentUserService currentUserService,
                       TaskDependencyRepository taskDependencyRepository, TaskAssignmentRepository taskAssignmentRepository) {
        this.tasks = tasks;
        this.projects = projects;
        this.currentUserService = currentUserService;
        this.taskDependencyRepository = taskDependencyRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
    }

    public TaskResponse createTask(Long projectId, CreateTaskRequest r) {
        Long userId = currentUserService.getCurrentUserId();
        ownedProject(userId, projectId);
        Task t = new Task();
        t.setProjectId(projectId);
        apply(t, r.title(), r.description(), r.status() == null ? TaskStatus.TODO : r.status(), r.priority() == null ? TaskPriority.MEDIUM : r.priority(), r.estimatedHours(), r.actualHours(), r.startDate(), r.dueDate(), r.progressPercentage() == null ? 0 : r.progressPercentage());
        return response(tasks.save(t));
    }

    @Transactional(readOnly = true)
    public List<TaskSummaryResponse> listTasksForProject(Long projectId) {
        Long userId = currentUserService.getCurrentUserId();
        ownedProject(userId, projectId);
        return tasks.findAllByProjectIdOrderByDueDateAsc(projectId).stream()
                .map(t -> new TaskSummaryResponse(t.getId(), t.getTitle(), t.getStatus(), t.getPriority(), t.getDueDate(), t.getProgressPercentage()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long taskId) {
        Long userId = currentUserService.getCurrentUserId();
        return response(ownedTask(userId, taskId));
    }

    public TaskResponse updateTask(Long id, UpdateTaskRequest r) {
        Long userId = currentUserService.getCurrentUserId();
        Task t = ownedTask(userId, id);
        apply(t, r.title(), r.description(), r.status(), r.priority(), r.estimatedHours(), r.actualHours(), r.startDate(), r.dueDate(), r.progressPercentage());
        return response(t);
    }

    public void deleteTask(Long id) {
        Long userId = currentUserService.getCurrentUserId();
        Task t = ownedTask(userId, id);
        
        // Cascading deletes
        taskDependencyRepository.deleteByPredecessorTaskIdOrSuccessorTaskId(id, id);
        taskAssignmentRepository.deleteByTaskId(id);
        
        tasks.delete(t);
    }

    public TaskResponse changeTaskStatus(Long id, TaskStatus status) {
        Long userId = currentUserService.getCurrentUserId();
        Task t = ownedTask(userId, id);
        t.setStatus(status);
        if (status == TaskStatus.DONE) t.setProgressPercentage(100);
        return response(t);
    }

    public TaskResponse changeTaskPriority(Long id, TaskPriority priority) {
        Long userId = currentUserService.getCurrentUserId();
        Task t = ownedTask(userId, id);
        t.setPriority(priority);
        return response(t);
    }

    public TaskResponse changeTaskProgress(Long id, Integer progressPercentage) {
        Long userId = currentUserService.getCurrentUserId();
        Task t = ownedTask(userId, id);
        t.setProgressPercentage(progressPercentage);
        if (progressPercentage == 100) {
            t.setStatus(TaskStatus.DONE);
        } else if (t.getStatus() == TaskStatus.DONE && progressPercentage < 100) {
            t.setStatus(TaskStatus.IN_PROGRESS);
        }
        return response(t);
    }

    private Project ownedProject(Long u, Long id) {
        Project p = projects.findById(id).orElseThrow(() -> new NotFoundException("Project not found"));
        if (!p.getOwnerId().equals(u)) throw new ForbiddenException("You do not have access to this project");
        return p;
    }

    private Task ownedTask(Long u, Long id) {
        Task t = tasks.findById(id).orElseThrow(() -> new NotFoundException("Task not found"));
        ownedProject(u, t.getProjectId());
        return t;
    }


    private void apply(Task t, String title, String description, TaskStatus status, TaskPriority priority, java.math.BigDecimal estimated, java.math.BigDecimal actual, java.time.LocalDate start, java.time.LocalDate due, Integer progress) {
        if (start != null && due != null && due.isBefore(start)) {
            throw new IllegalArgumentException("Due date must be on or after start date");
        }
        t.setTitle(title);
        t.setDescription(description);
        t.setStatus(status);
        t.setPriority(priority);
        t.setEstimatedHours(estimated);
        t.setActualHours(actual);
        t.setStartDate(start);
        t.setDueDate(due);
        t.setProgressPercentage(progress);
    }

    private TaskResponse response(Task t) {
        return new TaskResponse(t.getId(), t.getProjectId(), t.getTitle(), t.getDescription(), t.getStatus(), t.getPriority(), t.getEstimatedHours(), t.getActualHours(), t.getStartDate(), t.getDueDate(), t.getProgressPercentage(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
