package com.example.aiprojectmanager.team.controller;

import com.example.aiprojectmanager.auth.CurrentUserService;
import com.example.aiprojectmanager.team.dto.AddSkillRequest;
import com.example.aiprojectmanager.team.dto.CreateTeamMemberRequest;
import com.example.aiprojectmanager.team.dto.SkillDTO;
import com.example.aiprojectmanager.team.dto.TeamMemberDTO;
import com.example.aiprojectmanager.team.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TeamController {
    
    private final TeamService teamService;
    private final CurrentUserService currentUserService;

    @PostMapping("/projects/{projectId}/team-members")
    public ResponseEntity<TeamMemberDTO> addTeamMember(
            @PathVariable Long projectId,
            @RequestBody CreateTeamMemberRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(teamService.addTeamMember(projectId, request, userId));
    }

    @GetMapping("/projects/{projectId}/team-members")
    public ResponseEntity<List<TeamMemberDTO>> getTeamMembers(
            @PathVariable Long projectId) {
        Long userId = currentUserService.getCurrentUserId();
        return ResponseEntity.ok(teamService.getTeamMembers(projectId, userId));
    }

    @DeleteMapping("/projects/{projectId}/team-members/{memberId}")
    public ResponseEntity<Void> deleteTeamMember(
            @PathVariable Long projectId,
            @PathVariable Long memberId) {
        Long userId = currentUserService.getCurrentUserId();
        teamService.deleteTeamMember(projectId, memberId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/projects/{projectId}/team-members/{memberId}/skills")
    public ResponseEntity<Void> addSkill(
            @PathVariable Long projectId,
            @PathVariable Long memberId,
            @RequestBody AddSkillRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        teamService.addSkillToMember(projectId, memberId, request, userId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/skills")
    public ResponseEntity<List<SkillDTO>> getAllSkills() {
        return ResponseEntity.ok(teamService.getAllSkills());
    }
}
