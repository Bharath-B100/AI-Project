package com.example.aiprojectmanager.assignment.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TaskAssignmentDTO {
    private Long id;
    private Long taskId;
    private Long teamMemberId;
    private String teamMemberName;
    private BigDecimal allocationPercentage;
    private BigDecimal plannedHours;
    private BigDecimal actualHours;
    private LocalDateTime assignedAt;
}
