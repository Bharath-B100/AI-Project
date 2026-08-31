package com.example.aiprojectmanager.scheduling.dto;

import java.time.LocalDate;

/** A single task entry within a CriticalPathResponse. */
public record CriticalPathTaskItem(
        Long id,
        String name,
        Integer durationDays,
        LocalDate scheduledStart,
        LocalDate scheduledEnd
) {}
