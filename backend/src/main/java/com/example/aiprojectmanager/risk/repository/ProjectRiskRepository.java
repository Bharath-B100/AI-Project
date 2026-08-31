package com.example.aiprojectmanager.risk.repository;

import com.example.aiprojectmanager.risk.domain.ProjectRisk;
import com.example.aiprojectmanager.risk.domain.RiskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRiskRepository extends JpaRepository<ProjectRisk, Long> {
    List<ProjectRisk> findByProjectIdOrderByDetectedAtDesc(Long projectId);
    
    // Useful to prevent duplicates: find by project and riskType (and maybe task id in evidence, but for MVP one active risk per type is fine)
    Optional<ProjectRisk> findByProjectIdAndRiskType(Long projectId, RiskType riskType);
    void deleteByProjectId(Long projectId);
}
