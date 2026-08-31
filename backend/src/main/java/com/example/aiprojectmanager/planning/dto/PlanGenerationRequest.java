package com.example.aiprojectmanager.planning.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanGenerationRequest {

    @NotBlank(message = "Prompt must not be blank")
    private String prompt;

    private Integer timelineMonths;

    private Integer teamSize;

    private BigDecimal budget;

    private String methodology; // AGILE, WATERFALL, HYBRID
}
