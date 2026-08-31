package com.example.aiprojectmanager.planning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestedDependencyDto {
    private Long predecessorTaskId;
    private String predecessorTitle;
    private Long successorTaskId;
    private String successorTitle;
    private String dependencyType;
    private String rationale;
    private double confidenceScore;
}
