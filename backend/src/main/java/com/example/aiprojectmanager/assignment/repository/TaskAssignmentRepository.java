package com.example.aiprojectmanager.assignment.repository;

import com.example.aiprojectmanager.assignment.domain.TaskAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {
    List<TaskAssignment> findByTaskId(Long taskId);
    List<TaskAssignment> findByTeamMemberId(Long teamMemberId);
    Optional<TaskAssignment> findByTaskIdAndTeamMemberId(Long taskId, Long teamMemberId);
    
    // Using JPQL to find all assignments for a project
    @org.springframework.data.jpa.repository.Query("SELECT ta FROM TaskAssignment ta JOIN ta.task t WHERE t.projectId = :projectId")
    List<TaskAssignment> findByProjectId(Long projectId);

    void deleteByTaskId(Long taskId);
    void deleteByTeamMemberId(Long teamMemberId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM TaskAssignment ta WHERE ta.task IN (SELECT t FROM Task t WHERE t.projectId = :projectId)")
    void deleteByProjectId(Long projectId);
}
