package com.example.aiprojectmanager.project.dto; import com.example.aiprojectmanager.project.domain.ProjectStatus; import java.time.LocalDate;
public record ProjectSummaryResponse(Long id, String name, ProjectStatus status, LocalDate startDate, LocalDate endDate) {}
