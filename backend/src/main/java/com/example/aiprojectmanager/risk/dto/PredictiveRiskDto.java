package com.example.aiprojectmanager.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictiveRiskDto {

    private Long projectId;
    private String projectName;

    // Delay probability metric
    private Double delayProbabilityPercentage; // e.g. 42.0%
    private String riskLevel;                   // LOW, MODERATE, HIGH, CRITICAL
    private Integer predictedDelayDays;          // e.g. +7 days

    // Monte Carlo distribution percentiles
    private LocalDate p10FinishDate;            // Best Case (10th percentile)
    private LocalDate p50FinishDate;            // Expected Case (50th percentile)
    private LocalDate p90FinishDate;            // Pessimistic Buffer (90th percentile)

    // Key predictive drivers
    @Builder.Default
    private List<String> topRiskDrivers = new ArrayList<>();

    // Historical benchmark reference
    private Integer similarHistoricalProjectsCount;
    private String similarityAssessment;

    // Prescriptive Action
    private String recommendedRemediation;
}
