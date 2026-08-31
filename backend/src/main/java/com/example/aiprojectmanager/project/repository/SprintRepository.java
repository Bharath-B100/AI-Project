package com.example.aiprojectmanager.project.repository;

import com.example.aiprojectmanager.project.domain.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, Long> {
    List<Sprint> findByProjectIdOrderByStartDateAsc(Long projectId);
    Optional<Sprint> findByProjectIdAndStatus(Long projectId, String status);
    boolean existsByProjectIdAndId(Long projectId, Long sprintId);
}
