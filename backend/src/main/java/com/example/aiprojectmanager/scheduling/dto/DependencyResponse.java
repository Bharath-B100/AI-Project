package com.example.aiprojectmanager.scheduling.dto;

import com.example.aiprojectmanager.task.domain.DependencyType;

/** Response representing a single dependency edge. */
public record DependencyResponse(
        Long id,
        Long projectId,
        Long predecessorTaskId,
        Long successorTaskId,
        DependencyType dependencyType,
        Integer lagDays
) {}
