package com.example.aiprojectmanager.task.controller;

import com.example.aiprojectmanager.task.dto.*;
import com.example.aiprojectmanager.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
public class TaskController {
    private final TaskService service;

    public TaskController(TaskService s) {
        this.service = s;
    }

    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponse> create(@PathVariable Long projectId, @Valid @RequestBody CreateTaskRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createTask(projectId, r));
    }

    @GetMapping("/projects/{projectId}/tasks")
    public List<TaskSummaryResponse> list(@PathVariable Long projectId) {
        return service.listTasksForProject(projectId);
    }

    @GetMapping("/tasks/{taskId}")
    public TaskResponse get(@PathVariable Long taskId) {
        return service.getTaskById(taskId);
    }

    @PutMapping("/tasks/{taskId}")
    public TaskResponse update(@PathVariable Long taskId, @Valid @RequestBody UpdateTaskRequest r) {
        return service.updateTask(taskId, r);
    }

    @DeleteMapping("/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long taskId) {
        service.deleteTask(taskId);
    }

    @PatchMapping("/tasks/{taskId}/status")
    public TaskResponse status(@PathVariable Long taskId, @Valid @RequestBody TaskStatusRequest r) {
        return service.changeTaskStatus(taskId, r.status());
    }

    @PatchMapping("/tasks/{taskId}/priority")
    public TaskResponse priority(@PathVariable Long taskId, @Valid @RequestBody TaskPriorityRequest r) {
        return service.changeTaskPriority(taskId, r.priority());
    }

    @PatchMapping("/tasks/{taskId}/progress")
    public TaskResponse progress(@PathVariable Long taskId, @Valid @RequestBody TaskProgressRequest r) {
        return service.changeTaskProgress(taskId, r.progressPercentage());
    }
}
