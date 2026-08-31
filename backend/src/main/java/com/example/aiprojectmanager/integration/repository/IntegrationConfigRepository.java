package com.example.aiprojectmanager.integration.repository;

import com.example.aiprojectmanager.integration.domain.IntegrationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IntegrationConfigRepository extends JpaRepository<IntegrationConfig, Long> {
    List<IntegrationConfig> findByProjectId(Long projectId);
    Optional<IntegrationConfig> findByProjectIdAndProvider(Long projectId, String provider);
    boolean existsByProjectIdAndProvider(Long projectId, String provider);
}
