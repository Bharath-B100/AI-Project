package com.example.aiprojectmanager.team.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class TeamMemberDTO {
    private Long id;
    private Long projectId;
    private String name;
    private String email;
    private String role;
    private String timezone;
    private BigDecimal hourlyRate;
    private BigDecimal availabilityHoursPerWeek;
    private boolean active;
    private List<SkillDTO> skills;
}
