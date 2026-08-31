package com.example.aiprojectmanager.tracking.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProjectWorkloadDTO {
    private Long projectId;
    private List<TeamMemberWorkloadDTO> teamWorkloads;
}
