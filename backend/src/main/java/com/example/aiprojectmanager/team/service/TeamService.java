package com.example.aiprojectmanager.team.service;

import com.example.aiprojectmanager.project.domain.Project;
import com.example.aiprojectmanager.project.repository.ProjectRepository;
import com.example.aiprojectmanager.team.domain.Skill;
import com.example.aiprojectmanager.team.domain.TeamMember;
import com.example.aiprojectmanager.team.domain.TeamMemberSkill;
import com.example.aiprojectmanager.team.dto.AddSkillRequest;
import com.example.aiprojectmanager.team.dto.CreateTeamMemberRequest;
import com.example.aiprojectmanager.team.dto.SkillDTO;
import com.example.aiprojectmanager.team.dto.TeamMemberDTO;
import com.example.aiprojectmanager.team.repository.SkillRepository;
import com.example.aiprojectmanager.team.repository.TeamMemberRepository;
import com.example.aiprojectmanager.team.repository.TeamMemberSkillRepository;
import com.example.aiprojectmanager.assignment.repository.TaskAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {
    private final TeamMemberRepository teamMemberRepository;
    private final SkillRepository skillRepository;
    private final TeamMemberSkillRepository teamMemberSkillRepository;
    private final ProjectRepository projectRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;

    @Transactional
    public TeamMemberDTO addTeamMember(Long projectId, CreateTeamMemberRequest request, Long ownerId) {
        Project project = projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));

        TeamMember member = new TeamMember();
        member.setProject(project);
        member.setName(request.getName());
        member.setEmail(request.getEmail());
        member.setRole(request.getRole());
        member.setTimezone(request.getTimezone());
        member.setHourlyRate(request.getHourlyRate());
        if (request.getAvailabilityHoursPerWeek() != null) {
            member.setAvailabilityHoursPerWeek(request.getAvailabilityHoursPerWeek());
        }

        member = teamMemberRepository.save(member);
        return mapToDTO(member);
    }

    @Transactional(readOnly = true)
    public List<TeamMemberDTO> getTeamMembers(Long projectId, Long ownerId) {
        Project project = projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));

        return teamMemberRepository.findByProjectId(projectId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteTeamMember(Long projectId, Long memberId, Long ownerId) {
        Project project = projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));
                
        TeamMember member = teamMemberRepository.findByIdAndProjectId(memberId, projectId)
                .orElseThrow(() -> new IllegalArgumentException("Team member not found in this project"));

        taskAssignmentRepository.deleteByTeamMemberId(memberId);
        teamMemberSkillRepository.deleteByTeamMemberId(memberId);

        teamMemberRepository.delete(member);
    }

    @Transactional
    public void addSkillToMember(Long projectId, Long memberId, AddSkillRequest request, Long ownerId) {
        Project project = projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));

        TeamMember member = teamMemberRepository.findByIdAndProjectId(memberId, projectId)
                .orElseThrow(() -> new IllegalArgumentException("Team member not found in this project"));

        Skill skill = skillRepository.findAll().stream()
                .filter(s -> s.getName().equalsIgnoreCase(request.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Skill newSkill = new Skill();
                    newSkill.setName(request.getName());
                    return skillRepository.save(newSkill);
                });

        TeamMemberSkill memberSkill = new TeamMemberSkill();
        memberSkill.setTeamMember(member);
        memberSkill.setSkill(skill);
        memberSkill.setProficiencyLevel(request.getProficiencyLevel());

        teamMemberSkillRepository.save(memberSkill);
    }
    
    @Transactional(readOnly = true)
    public List<SkillDTO> getAllSkills() {
        return skillRepository.findAll().stream().map(s -> {
            SkillDTO dto = new SkillDTO();
            dto.setId(s.getId());
            dto.setName(s.getName());
            dto.setDescription(s.getDescription());
            return dto;
        }).collect(Collectors.toList());
    }

    private TeamMemberDTO mapToDTO(TeamMember member) {
        TeamMemberDTO dto = new TeamMemberDTO();
        dto.setId(member.getId());
        dto.setProjectId(member.getProject().getId());
        dto.setName(member.getName());
        dto.setEmail(member.getEmail());
        dto.setRole(member.getRole());
        dto.setTimezone(member.getTimezone());
        dto.setHourlyRate(member.getHourlyRate());
        dto.setAvailabilityHoursPerWeek(member.getAvailabilityHoursPerWeek());
        dto.setActive(member.isActive());
        
        List<SkillDTO> skills = teamMemberSkillRepository.findByTeamMemberId(member.getId()).stream().map(tms -> {
            SkillDTO sd = new SkillDTO();
            sd.setId(tms.getSkill().getId());
            sd.setName(tms.getSkill().getName());
            sd.setDescription(tms.getSkill().getDescription());
            sd.setProficiencyLevel(tms.getProficiencyLevel().name());
            return sd;
        }).collect(Collectors.toList());
        dto.setSkills(skills);
        
        return dto;
    }
}
