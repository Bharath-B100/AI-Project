package com.example.aiprojectmanager.planning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedMilestoneDto {

    private String name;

    private String description;

    private Integer targetDayOffset;
}
