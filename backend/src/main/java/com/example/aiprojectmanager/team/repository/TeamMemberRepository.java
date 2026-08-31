package com.example.aiprojectmanager.team.repository;

import com.example.aiprojectmanager.team.domain.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findByProjectId(Long projectId);
    Optional<TeamMember> findByIdAndProjectId(Long id, Long projectId);
    void deleteByProjectId(Long projectId);
}
