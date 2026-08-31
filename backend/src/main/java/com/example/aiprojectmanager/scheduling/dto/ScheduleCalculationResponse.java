package com.example.aiprojectmanager.scheduling.dto;

import java.util.List;

/** Response returned from POST /projects/{id}/schedule/calculate. */
public record ScheduleCalculationResponse(
        /** All project tasks with their newly computed scheduled dates. */
        List<GanttTaskItem> calculatedTasks,
        /** IDs of critical-path tasks, in topological order. */
        List<Long> criticalPath,
        /** Minimum project duration = sum of critical-path task durations. */
        Integer totalDurationDays
) {}
