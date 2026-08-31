package com.example.aiprojectmanager.risk.service;

import com.example.aiprojectmanager.project.repository.ProjectRepository;
import com.example.aiprojectmanager.risk.domain.ProjectRisk;
import com.example.aiprojectmanager.risk.dto.ProjectRiskDTO;
import com.example.aiprojectmanager.risk.dto.UpdateRiskStatusRequest;
import com.example.aiprojectmanager.risk.repository.ProjectRiskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RiskService {

    private final RiskRuleEngine riskRuleEngine;
    private final ProjectRiskRepository riskRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public void runRiskAnalysis(Long projectId, Long ownerId) {
        projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));
        riskRuleEngine.analyzeRisks(projectId, ownerId);
    }

    @Transactional(readOnly = true)
    public List<ProjectRiskDTO> getProjectRisks(Long projectId, Long ownerId) {
        projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));
                
        return riskRepository.findByProjectIdOrderByDetectedAtDesc(projectId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectRiskDTO updateRiskStatus(Long projectId, Long riskId, UpdateRiskStatusRequest request, Long ownerId) {
        projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));

        ProjectRisk risk = riskRepository.findById(riskId)
                .orElseThrow(() -> new IllegalArgumentException("Risk not found"));

        if (!risk.getProject().getId().equals(projectId)) {
            throw new IllegalArgumentException("Risk does not belong to this project");
        }

        risk.setStatus(request.getStatus());
        risk = riskRepository.save(risk);
        return mapToDTO(risk);
    }

    private ProjectRiskDTO mapToDTO(ProjectRisk risk) {
        ProjectRiskDTO dto = new ProjectRiskDTO();
        dto.setId(risk.getId());
        dto.setProjectId(risk.getProject().getId());
        dto.setRiskType(risk.getRiskType().name());
        dto.setSeverity(risk.getSeverity().name());
        dto.setRiskScore(risk.getRiskScore());
        dto.setTitle(risk.getTitle());
        dto.setDescription(risk.getDescription());
        dto.setEvidenceJson(risk.getEvidenceJson());
        dto.setSuggestedAction(risk.getSuggestedAction());
        dto.setStatus(risk.getStatus().name());
        dto.setDetectedAt(risk.getDetectedAt());
        return dto;
    }
}
