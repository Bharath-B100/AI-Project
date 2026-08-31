package com.example.aiprojectmanager.scheduling.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoLevelResponse {
    private Long projectId;
    private int totalTasks;
    private int leveledTasks;
    private int resolvedResourceConflicts;
    private LocalDate originalProjectEnd;
    private LocalDate leveledProjectEnd;
    private int delayOrSavedDays;
    private List<GanttTaskItem> tasks;
    private List<String> levelingLog;
}
