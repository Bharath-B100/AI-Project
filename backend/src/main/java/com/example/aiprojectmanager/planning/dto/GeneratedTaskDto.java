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
public class GeneratedTaskDto {

    private String tempId;              // e.g. "T1", "T2"

    private String title;

    private String description;

    private BigDecimal estimatedHours;

    private Integer durationDays;

    private String priority;            // HIGH, MEDIUM, LOW

    private String milestone;           // e.g. "Phase 1: Architecture & UI/UX"

    @Builder.Default
    private List<String> requiredSkills = new ArrayList<>();

    @Builder.Default
    private List<String> dependsOnTempIds = new ArrayList<>(); // e.g. ["T1"]
}
