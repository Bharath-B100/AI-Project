package com.example.aiprojectmanager.risk.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProjectRiskDTO {
    private Long id;
    private Long projectId;
    private String riskType;
    private String severity;
    private Integer riskScore;
    private String title;
    private String description;
    private String evidenceJson;
    private String suggestedAction;
    private String status;
    private LocalDateTime detectedAt;
}
