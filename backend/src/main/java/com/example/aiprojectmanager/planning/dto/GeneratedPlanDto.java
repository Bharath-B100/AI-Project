package com.example.aiprojectmanager.planning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedPlanDto {

    private String projectName;

    private String description;

    private String suggestedMethodology;

    private Integer estimatedTotalDays;

    private BigDecimal recommendedBudget;

    @Builder.Default
    private List<String> recommendedRoles = new ArrayList<>();

    @Builder.Default
    private List<GeneratedMilestoneDto> milestones = new ArrayList<>();

    @Builder.Default
    private List<GeneratedTaskDto> tasks = new ArrayList<>();
}
