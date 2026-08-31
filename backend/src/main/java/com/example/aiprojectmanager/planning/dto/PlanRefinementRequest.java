package com.example.aiprojectmanager.planning.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanRefinementRequest {
    @NotBlank(message = "Refinement instruction cannot be blank")
    private String instruction;
    
    private GeneratedPlanDto currentPlan;
}
