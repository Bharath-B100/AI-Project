package com.example.aiprojectmanager.scheduling.dto;

import com.example.aiprojectmanager.task.domain.DependencyType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for creating a task dependency edge.
 * The successorTaskId is provided as a path variable at the controller layer,
 * but is also accepted in the body for direct service calls.
 */
public record CreateDependencyRequest(
        @NotNull(message = "predecessorTaskId is required")
        Long predecessorTaskId,

        /** Optional — overridden by the path variable taskId in the controller. */
        Long successorTaskId,

        DependencyType dependencyType,

        @Min(value = 0, message = "lagDays must be >= 0")
        Integer lagDays
) {}
