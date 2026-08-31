package com.example.aiprojectmanager.tracking.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProjectProgressDTO {
    private BigDecimal actualProgress;
    private BigDecimal expectedProgress;
    private BigDecimal progressVariance;
    private int totalTasks;
    private int completedTasks;
    private int overdueTasks;
    private String projectHealth; // ON_TRACK, AT_RISK, OFF_TRACK
}
