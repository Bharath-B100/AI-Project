package com.example.aiprojectmanager.risk.controller;

import com.example.aiprojectmanager.auth.CurrentUserService;
import com.example.aiprojectmanager.risk.dto.ProjectRiskDTO;
import com.example.aiprojectmanager.risk.dto.UpdateRiskStatusRequest;
import com.example.aiprojectmanager.risk.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/risks")
@RequiredArgsConstructor
public class RiskController {

    private final RiskService riskService;
    private final CurrentUserService currentUserService;

    @PostMapping("/analyze")
    public ResponseEntity<Void> analyzeRisks(
            @PathVariable Long projectId) {
        Long userId = currentUserService.getCurrentUserId();
        riskService.runRiskAnalysis(projectId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<ProjectRiskDTO>> getRisks(
            @PathVariable Long projectId) {
        Long userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(riskService.getProjectRisks(projectId, userId));
    }

    @PatchMapping("/{riskId}/status")
    public ResponseEntity<ProjectRiskDTO> updateRiskStatus(
            @PathVariable Long projectId,
            @PathVariable Long riskId,
            @RequestBody UpdateRiskStatusRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(riskService.updateRiskStatus(projectId, riskId, request, userId));
    }
}
