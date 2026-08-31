package com.example.aiprojectmanager.team.dto;

import lombok.Data;
import com.example.aiprojectmanager.team.domain.SkillProficiency;

@Data
public class AddSkillRequest {
    private String name;
    private SkillProficiency proficiencyLevel;
}
