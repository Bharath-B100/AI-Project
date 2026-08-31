package com.example.aiprojectmanager.scheduling.controller;

import com.example.aiprojectmanager.scheduling.dto.*;
import com.example.aiprojectmanager.scheduling.service.GanttDataService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST endpoints for task dependencies, schedule calculation, and Gantt chart data.
 * All paths are automatically prefixed with /api/v1 by WebMvcConfig.
 */
@RestController
public class SchedulingController {

    private final GanttDataService ganttDataService;

    public SchedulingController(GanttDataService ganttDataService) {
        this.ganttDataService = ganttDataService;
    }

    // ── Dependencies ──────────────────────────────────────────────────────────

    /**
     * POST /api/v1/projects/{projectId}/tasks/{taskId}/dependencies
     * Creates a dependency where taskId is the SUCCESSOR and predecessorTaskId comes from the body.
     */
    @PostMapping("/projects/{projectId}/tasks/{taskId}/dependencies")
    public ResponseEntity<DependencyResponse> createDependency(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody CreateDependencyRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ganttDataService.createDependency(projectId, taskId, req));
    }

    /**
     * GET /api/v1/projects/{projectId}/dependencies
     * Returns all dependency edges for a project.
     */
    @GetMapping("/projects/{projectId}/dependencies")
    public List<DependencyResponse> listDependencies(@PathVariable Long projectId) {
        return ganttDataService.listDependencies(projectId);
    }

    /**
     * DELETE /api/v1/projects/{projectId}/dependencies/{dependencyId}
     */
    @DeleteMapping("/projects/{projectId}/dependencies/{dependencyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDependency(@PathVariable Long projectId,
                                 @PathVariable Long dependencyId) {
        ganttDataService.deleteDependency(projectId, dependencyId);
    }

    // ── Schedule Calculation ──────────────────────────────────────────────────

    /**
     * POST /api/v1/projects/{projectId}/schedule/calculate
     * Recalculates all task scheduled dates using CPM and returns the result.
     */
    @PostMapping("/projects/{projectId}/schedule/calculate")
    public ScheduleCalculationResponse calculateSchedule(@PathVariable Long projectId) {
        return ganttDataService.calculateSchedule(projectId);
    }

    // ── Gantt & Critical Path ─────────────────────────────────────────────────

    /**
     * GET /api/v1/projects/{projectId}/gantt
     * Returns Gantt chart data with CPM-computed dates.
     */
    @GetMapping("/projects/{projectId}/gantt")
    public GanttDataResponse getGanttData(@PathVariable Long projectId) {
        return ganttDataService.getGanttData(projectId);
    }

    /**
     * GET /api/v1/projects/{projectId}/critical-path
     * Returns the critical path tasks and total project duration.
     */
    @GetMapping("/projects/{projectId}/critical-path")
    public CriticalPathResponse getCriticalPath(@PathVariable Long projectId) {
        return ganttDataService.getCriticalPath(projectId);
    }

    /**
     * POST /api/v1/projects/{projectId}/schedule/auto-level
     * Performs resource-constrained auto-leveling CPM schedule calculation.
     */
    @PostMapping("/projects/{projectId}/schedule/auto-level")
    public AutoLevelResponse autoLevelSchedule(@PathVariable Long projectId) {
        return ganttDataService.autoLevelSchedule(projectId);
    }
}
