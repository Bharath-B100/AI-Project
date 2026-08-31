package com.example.aiprojectmanager.project.controller;

import com.example.aiprojectmanager.auth.CurrentUserService;
import com.example.aiprojectmanager.project.dto.SprintDTO;
import com.example.aiprojectmanager.project.service.SprintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Sprint management endpoints.
 *
 * GET    /api/v1/projects/{id}/sprints               — list sprints
 * POST   /api/v1/projects/{id}/sprints               — create sprint
 * POST   /api/v1/projects/{id}/sprints/{sid}/start   — start sprint
 * POST   /api/v1/projects/{id}/sprints/{sid}/complete — complete sprint
 * POST   /api/v1/projects/{id}/sprints/{sid}/tasks/{tid} — assign task to sprint
 */
@RestController
@RequestMapping("/projects/{projectId}/sprints")
@RequiredArgsConstructor
public class SprintController {

    private final SprintService        sprintService;
    private final CurrentUserService   currentUserService;

    @GetMapping
    public ResponseEntity<List<SprintDTO>> listSprints(@PathVariable Long projectId) {
        return ResponseEntity.ok(sprintService.listSprints(projectId, currentUserService.getCurrentUserId()));
    }

    @PostMapping
    public ResponseEntity<SprintDTO> createSprint(@PathVariable Long projectId,
                                                   @RequestBody SprintDTO req) {
        SprintDTO created = sprintService.createSprint(projectId, currentUserService.getCurrentUserId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{sprintId}/start")
    public ResponseEntity<SprintDTO> startSprint(@PathVariable Long projectId,
                                                  @PathVariable Long sprintId) {
        return ResponseEntity.ok(sprintService.startSprint(projectId, sprintId, currentUserService.getCurrentUserId()));
    }

    @PostMapping("/{sprintId}/complete")
    public ResponseEntity<SprintDTO> completeSprint(@PathVariable Long projectId,
                                                     @PathVariable Long sprintId) {
        return ResponseEntity.ok(sprintService.completeSprint(projectId, sprintId, currentUserService.getCurrentUserId()));
    }

    @PostMapping("/{sprintId}/tasks/{taskId}")
    public ResponseEntity<Map<String, String>> assignTask(@PathVariable Long projectId,
                                                           @PathVariable Long sprintId,
                                                           @PathVariable Long taskId) {
        sprintService.assignTaskToSprint(projectId, sprintId, taskId, currentUserService.getCurrentUserId());
        return ResponseEntity.ok(Map.of("message", "Task assigned to sprint successfully"));
    }
}
