package com.example.aiprojectmanager.scheduling.dto;

import java.util.List;

/** Full critical-path analysis result for a project. */
public record CriticalPathResponse(
        /** Tasks on the critical path, in topological execution order. */
        List<CriticalPathTaskItem> tasks,
        /** Sum of durations along the critical path (= minimum project duration). */
        Integer totalDurationDays
) {}
