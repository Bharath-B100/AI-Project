package com.example.aiprojectmanager.tracking.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TeamMemberWorkloadDTO {
    private Long teamMemberId;
    private String teamMemberName;
    private int assignedTaskCount;
    private BigDecimal plannedHours;
    private BigDecimal actualHours;
    private BigDecimal availableHours;
    private BigDecimal utilizationPercentage;
    private String workloadStatus; // AVAILABLE, NEAR_CAPACITY, OVERLOADED
}
