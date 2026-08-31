package com.example.aiprojectmanager.ai.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Domain benchmark profile trained on empirical project delivery datasets.
 * Sources: Standish Group CHAOS Report, IEEE Software Engineering Metrics, Scrum Alliance Velocity Surveys.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainBenchmarkProfile {
    private String domainName;
    private double optimisticDurationRatio;
    private double pessimisticDurationRatio;
    private double averageHistoricalDelayRiskPct;
    private double complianceOverheadMultiplier;
    private double scopeCreepProbability;
    private List<String> primaryRiskFactors;
    private List<String> standardSkillKeywords;
    private Map<String, Double> phaseDistributionPct;
}
