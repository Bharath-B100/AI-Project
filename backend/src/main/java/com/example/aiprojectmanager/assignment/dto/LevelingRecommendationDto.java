package com.example.aiprojectmanager.assignment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LevelingRecommendationDto {

    private Long taskId;
    private String taskTitle;
    private Integer taskDurationDays;
    private BigDecimal plannedHours;

    private Long sourceMemberId;
    private String sourceMemberName;
    private BigDecimal sourceCurrentWorkloadPct;

    private Long targetMemberId;
    private String targetMemberName;
    private BigDecimal targetCurrentWorkloadPct;

    private String rationale;
}
