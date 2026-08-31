package com.example.aiprojectmanager.team.repository;

import com.example.aiprojectmanager.team.domain.TeamMemberSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamMemberSkillRepository extends JpaRepository<TeamMemberSkill, Long> {
    List<TeamMemberSkill> findByTeamMemberId(Long teamMemberId);
    void deleteByTeamMemberId(Long teamMemberId);
}
