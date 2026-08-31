package com.example.aiprojectmanager.project.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class SprintDTO {
    private Long      id;
    private Long      projectId;
    private String    name;
    private String    goal;
    private LocalDate startDate;
    private LocalDate endDate;
    private String    status;
    private Integer   velocityPoints;
    // Computed on read
    private int       totalTasks;
    private int       completedTasks;
    private int       totalStoryPoints;
    private int       completedStoryPoints;
    private double    completionPct;
}
