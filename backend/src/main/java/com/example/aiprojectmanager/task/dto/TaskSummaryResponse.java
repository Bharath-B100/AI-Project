package com.example.aiprojectmanager.task.dto; import com.example.aiprojectmanager.task.domain.*; import java.time.LocalDate;
public record TaskSummaryResponse(Long id, String title, TaskStatus status, TaskPriority priority, LocalDate dueDate, Integer progressPercentage) {}
