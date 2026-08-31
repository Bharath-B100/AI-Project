package com.example.aiprojectmanager.dashboard.dto;

import com.example.aiprojectmanager.project.domain.Project;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardOverviewDTO {
    private int totalProjects;
    private int activeProjects;
    private BigDecimal totalBudget;
    private BigDecimal totalSpent;
    private String globalHealthStatus; // ON_TRACK, AT_RISK, OFF_TRACK, IDLE
    private String globalWorkloadStatus; // AVAILABLE, NEAR_CAPACITY, OVERLOADED, IDLE
    private List<Project> recentProjects;
}
