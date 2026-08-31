package com.example.aiprojectmanager.simulation.dto;

import com.example.aiprojectmanager.scheduling.dto.GanttTaskItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResultDto {

    private Long projectId;

    // Baseline metrics
    private Integer baselineDurationDays;
    private LocalDate baselineFinishDate;
    private BigDecimal baselineEstimatedCost;
    private Integer baselineCriticalPathLength;

    // Simulated metrics
    private Integer simulatedDurationDays;
    private LocalDate simulatedFinishDate;
    private BigDecimal simulatedEstimatedCost;
    private Integer simulatedCriticalPathLength;

    // Comparison deltas
    private Integer durationDeltaDays;       // negative is faster, positive is delayed
    private BigDecimal costDelta;            // cost difference
    private String feasibilityAssessment;   // FEASIBLE, HIGH_RISK, OPTIMAL, DIMINISHING_RETURNS

    @Builder.Default
    private List<String> prescriptiveRecommendations = new ArrayList<>();

    @Builder.Default
    private List<GanttTaskItem> simulatedTasks = new ArrayList<>();

    @Builder.Default
    private List<Long> simulatedCriticalPath = new ArrayList<>();
}
