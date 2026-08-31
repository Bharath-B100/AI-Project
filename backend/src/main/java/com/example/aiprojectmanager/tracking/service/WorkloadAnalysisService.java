package com.example.aiprojectmanager.tracking.service;

import com.example.aiprojectmanager.assignment.domain.TaskAssignment;
import com.example.aiprojectmanager.assignment.repository.TaskAssignmentRepository;
import com.example.aiprojectmanager.project.domain.Project;
import com.example.aiprojectmanager.project.repository.ProjectRepository;
import com.example.aiprojectmanager.team.domain.TeamMember;
import com.example.aiprojectmanager.team.repository.TeamMemberRepository;
import com.example.aiprojectmanager.tracking.dto.ProjectWorkloadDTO;
import com.example.aiprojectmanager.tracking.dto.TeamMemberWorkloadDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkloadAnalysisService {

    private final ProjectRepository projectRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;

    @Transactional(readOnly = true)
    public ProjectWorkloadDTO getProjectWorkload(Long projectId, Long ownerId) {
        Project project = projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));

        List<TeamMember> members = teamMemberRepository.findByProjectId(projectId);
        List<TeamMemberWorkloadDTO> teamWorkloads = new ArrayList<>();

        for (TeamMember member : members) {
            teamWorkloads.add(getTeamMemberWorkload(project, member));
        }

        ProjectWorkloadDTO dto = new ProjectWorkloadDTO();
        dto.setProjectId(projectId);
        dto.setTeamWorkloads(teamWorkloads);
        return dto;
    }

    private TeamMemberWorkloadDTO getTeamMemberWorkload(Project project, TeamMember member) {
        List<TaskAssignment> assignments = taskAssignmentRepository.findByTeamMemberId(member.getId());

        BigDecimal totalPlannedHours = BigDecimal.ZERO;
        BigDecimal totalActualHours = BigDecimal.ZERO;
        int assignedTaskCount = assignments.size();

        for (TaskAssignment assignment : assignments) {
            if (assignment.getPlannedHours() != null) {
                totalPlannedHours = totalPlannedHours.add(assignment.getPlannedHours());
            }
            if (assignment.getActualHours() != null) {
                totalActualHours = totalActualHours.add(assignment.getActualHours());
            }
        }

        // Calculate available hours during the project period
        BigDecimal availableHours = BigDecimal.ZERO;
        if (project.getStartDate() != null && project.getEndDate() != null && member.getAvailabilityHoursPerWeek() != null) {
            long totalDays = ChronoUnit.DAYS.between(project.getStartDate(), project.getEndDate());
            if (totalDays > 0) {
                BigDecimal weeks = BigDecimal.valueOf(totalDays).divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP);
                availableHours = member.getAvailabilityHoursPerWeek().multiply(weeks);
            }
        }

        BigDecimal utilizationPercentage = BigDecimal.ZERO;
        String status = "AVAILABLE";
        
        if (availableHours.compareTo(BigDecimal.ZERO) > 0) {
            utilizationPercentage = totalPlannedHours.divide(availableHours, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
            
            if (utilizationPercentage.compareTo(BigDecimal.valueOf(100)) > 0) {
                status = "OVERLOADED";
            } else if (utilizationPercentage.compareTo(BigDecimal.valueOf(80)) >= 0) {
                status = "NEAR_CAPACITY";
            }
        } else if (totalPlannedHours.compareTo(BigDecimal.ZERO) > 0) {
             status = "OVERLOADED"; // Has tasks but no available hours
             utilizationPercentage = BigDecimal.valueOf(100.00);
        }

        TeamMemberWorkloadDTO dto = new TeamMemberWorkloadDTO();
        dto.setTeamMemberId(member.getId());
        dto.setTeamMemberName(member.getName());
        dto.setAssignedTaskCount(assignedTaskCount);
        dto.setPlannedHours(totalPlannedHours);
        dto.setActualHours(totalActualHours);
        dto.setAvailableHours(availableHours);
        dto.setUtilizationPercentage(utilizationPercentage);
        dto.setWorkloadStatus(status);

        return dto;
    }
}
