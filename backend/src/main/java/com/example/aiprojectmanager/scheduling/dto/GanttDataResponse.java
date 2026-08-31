package com.example.aiprojectmanager.scheduling.dto;

import java.time.LocalDate;
import java.util.List;

/** Full Gantt chart payload for a project. */
public record GanttDataResponse(
        List<GanttTaskItem> tasks,
        LocalDate projectStart,
        LocalDate projectEnd,
        /** IDs of tasks on the critical path, in topological order. */
        List<Long> criticalPath
) {}
