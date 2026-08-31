package com.example.aiprojectmanager.tracking.controller;

import com.example.aiprojectmanager.auth.CurrentUserService;
import com.example.aiprojectmanager.tracking.domain.ProjectCostEntry;
import com.example.aiprojectmanager.tracking.dto.BudgetHealthDTO;
import com.example.aiprojectmanager.tracking.dto.CreateCostEntryRequest;
import com.example.aiprojectmanager.tracking.dto.ProjectProgressDTO;
import com.example.aiprojectmanager.tracking.dto.ProjectWorkloadDTO;
import com.example.aiprojectmanager.tracking.dto.TeamMemberWorkloadDTO;
import com.example.aiprojectmanager.tracking.service.BudgetTrackingService;
import com.example.aiprojectmanager.tracking.service.ProgressTrackingService;
import com.example.aiprojectmanager.tracking.service.WorkloadAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}")
@RequiredArgsConstructor
public class TrackingController {

    private final ProgressTrackingService progressService;
    private final WorkloadAnalysisService workloadService;
    private final BudgetTrackingService budgetService;
    private final CurrentUserService currentUserService;

    @GetMapping("/progress")
    public ResponseEntity<ProjectProgressDTO> getProjectProgress(
            @PathVariable Long projectId) {
        Long userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(progressService.calculateProjectProgress(projectId, userId, LocalDate.now()));
    }

    @GetMapping("/health")
    public ResponseEntity<ProjectProgressDTO> getProjectHealth(
            @PathVariable Long projectId) {
        // Return same as progress for MVP, as it contains projectHealth and variances
        Long userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(progressService.calculateProjectProgress(projectId, userId, LocalDate.now()));
    }

    @GetMapping("/workload")
    public ResponseEntity<ProjectWorkloadDTO> getProjectWorkload(
            @PathVariable Long projectId) {
        Long userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(workloadService.getProjectWorkload(projectId, userId));
    }
    
    @GetMapping("/workload/{teamMemberId}")
    public ResponseEntity<TeamMemberWorkloadDTO> getTeamMemberWorkload(
            @PathVariable Long projectId,
            @PathVariable Long teamMemberId) {
        Long userId = currentUserService.getCurrentUserId();
        ProjectWorkloadDTO projectWorkload = workloadService.getProjectWorkload(projectId, userId);
        
        TeamMemberWorkloadDTO memberWorkload = projectWorkload.getTeamWorkloads().stream()
                .filter(mw -> mw.getTeamMemberId().equals(teamMemberId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Team member not found in workload"));
                
        return ResponseEntity.ok(memberWorkload);
    }

    @PostMapping("/costs")
    public ResponseEntity<ProjectCostEntry> addCostEntry(
            @PathVariable Long projectId,
            @RequestBody CreateCostEntryRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(budgetService.addCostEntry(projectId, request, userId));
    }

    @GetMapping("/costs")
    public ResponseEntity<List<ProjectCostEntry>> getCosts(
            @PathVariable Long projectId) {
        Long userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(budgetService.listCostEntries(projectId, userId));
    }

    @GetMapping("/budget-health")
    public ResponseEntity<BudgetHealthDTO> getBudgetHealth(
            @PathVariable Long projectId) {
        Long userId = currentUserService.getCurrentUserId();
        ProjectProgressDTO progress = progressService.calculateProjectProgress(projectId, userId, LocalDate.now());
        return ResponseEntity.ok(budgetService.calculateBudgetHealth(projectId, userId, progress.getActualProgress()));
    }
}
