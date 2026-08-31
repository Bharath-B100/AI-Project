package com.example.aiprojectmanager.project.controller;

import com.example.aiprojectmanager.project.dto.*;
import com.example.aiprojectmanager.project.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/projects")
public class ProjectController {
    private final ProjectService service;

    public ProjectController(ProjectService s) {
        this.service = s;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createProject(r));
    }

    @GetMapping
    public List<ProjectSummaryResponse> list() {
        return service.listProjectsForUser();
    }

    @GetMapping("/{projectId}")
    public ProjectResponse get(@PathVariable Long projectId) {
        return service.getProjectById(projectId);
    }

    @PutMapping("/{projectId}")
    public ProjectResponse update(@PathVariable Long projectId, @Valid @RequestBody UpdateProjectRequest r) {
        return service.updateProject(projectId, r);
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long projectId) {
        service.deleteProject(projectId);
    }
}
