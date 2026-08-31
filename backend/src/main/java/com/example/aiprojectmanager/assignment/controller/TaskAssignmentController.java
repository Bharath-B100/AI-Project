package com.example.aiprojectmanager.assignment.controller;

import com.example.aiprojectmanager.assignment.dto.AssignTaskRequest;
import com.example.aiprojectmanager.assignment.dto.TaskAssignmentDTO;
import com.example.aiprojectmanager.assignment.service.TaskAssignmentService;
import com.example.aiprojectmanager.auth.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TaskAssignmentController {

    private final TaskAssignmentService taskAssignmentService;
    private final CurrentUserService currentUserService;

    @PostMapping("/tasks/{taskId}/assignments")
    public ResponseEntity<TaskAssignmentDTO> assignTask(
            @PathVariable Long taskId,
            @RequestBody AssignTaskRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(taskAssignmentService.assignTask(taskId, request, userId));
    }

    @GetMapping("/tasks/{taskId}/assignments")
    public ResponseEntity<List<TaskAssignmentDTO>> getTaskAssignments(
            @PathVariable Long taskId) {
        Long userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(taskAssignmentService.getTaskAssignments(taskId, userId));
    }

    @DeleteMapping("/tasks/{taskId}/assignments/{assignmentId}")
    public ResponseEntity<Void> removeAssignment(
            @PathVariable Long taskId,
            @PathVariable Long assignmentId) {
        Long userId = currentUserService.getCurrentUserId();
        taskAssignmentService.removeAssignment(taskId, assignmentId, userId);
        return ResponseEntity.noContent().build();
    }
}
