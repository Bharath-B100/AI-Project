package com.example.aiprojectmanager.tracking.repository;

import com.example.aiprojectmanager.tracking.domain.ProjectCostEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectCostEntryRepository extends JpaRepository<ProjectCostEntry, Long> {
    List<ProjectCostEntry> findByProjectIdOrderByEntryDateDesc(Long projectId);
}
