package com.example.aiprojectmanager.planning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommitPlanRequest {

    @NotBlank(message = "Project name must not be blank")
    private String projectName;

    private String description;

    private String methodology;

    private BigDecimal budget;

    private LocalDate startDate;

    @NotEmpty(message = "Plan must have at least one task")
    @Builder.Default
    private List<GeneratedTaskDto> tasks = new ArrayList<>();
}
