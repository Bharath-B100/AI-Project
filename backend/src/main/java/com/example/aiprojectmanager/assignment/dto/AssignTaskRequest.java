package com.example.aiprojectmanager.assignment.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AssignTaskRequest {
    private Long teamMemberId;
    private BigDecimal allocationPercentage;
    private BigDecimal plannedHours;
}
