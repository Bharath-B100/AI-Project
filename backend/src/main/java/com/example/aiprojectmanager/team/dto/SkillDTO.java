package com.example.aiprojectmanager.team.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SkillDTO {
    private Long id;
    private String name;
    private String description;
    private String proficiencyLevel; // Used when returning skills of a member
}
