package com.example.aiprojectmanager.scheduling.dto;

import com.example.aiprojectmanager.task.domain.TaskPriority;
import com.example.aiprojectmanager.task.domain.TaskStatus;
import java.time.LocalDate;
import java.util.List;

/**
 * A single task as it appears in a Gantt chart row.
 * {@code scheduledStart}/{@code scheduledEnd} are CPM-computed dates.
 * {@code isCritical} is true when the task's slack == 0.
 */
public record GanttTaskItem(
        Long id,
        String name,
        LocalDate scheduledStart,
        LocalDate scheduledEnd,
        Integer durationDays,
        Integer progressPercentage,
        /** IDs of predecessor tasks (dependency arrows). */
        List<Long> dependencies,
        boolean isCritical,
        TaskStatus status,
        TaskPriority priority
) {}
