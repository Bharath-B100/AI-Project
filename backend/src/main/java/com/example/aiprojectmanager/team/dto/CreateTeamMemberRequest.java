package com.example.aiprojectmanager.team.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateTeamMemberRequest {
    private String name;
    private String email;
    private String role;
    private String timezone;
    private BigDecimal hourlyRate;
    private BigDecimal availabilityHoursPerWeek;
}
