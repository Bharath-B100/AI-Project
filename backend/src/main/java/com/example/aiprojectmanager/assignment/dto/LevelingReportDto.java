package com.example.aiprojectmanager.assignment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LevelingReportDto {

    private Long projectId;
    private Integer totalTeamMembers;
    private Integer overloadedCount;
    private Integer availableCount;
    private String portfolioWorkloadStatus; // OPTIMAL, OVERLOADED, UNBALANCED

    @Builder.Default
    private List<LevelingRecommendationDto> recommendations = new ArrayList<>();
}
