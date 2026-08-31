package com.example.aiprojectmanager.team.repository;

import com.example.aiprojectmanager.team.domain.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
}
