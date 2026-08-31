package com.example.aiprojectmanager.simulation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSimulationOverride {
    private Long taskId;
    private Integer durationDeltaDays;   // e.g. +5 days or -2 days
    private Integer newDurationDays;
    private Boolean excludeFromScope;    // Cut feature / defer task
}
