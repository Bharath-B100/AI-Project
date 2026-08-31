package com.example.aiprojectmanager.simulation.dto;

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
public class SimulationRequest {

    /** Number of additional developers to add (e.g. +2) or remove (-1) */
    @Builder.Default
    private Integer developerDelta = 0;

    /** Assumed average hourly rate for newly added developers */
    @Builder.Default
    private BigDecimal developerHourlyRate = BigDecimal.valueOf(500);

    /** Percentage velocity boost/slowdown (e.g. +20% or -15%) */
    @Builder.Default
    private BigDecimal productivityMultiplier = BigDecimal.valueOf(1.0);

    /** Optional individual task duration overrides or scope cuts */
    @Builder.Default
    private List<TaskSimulationOverride> taskOverrides = new ArrayList<>();
}
