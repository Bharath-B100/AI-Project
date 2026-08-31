package com.example.aiprojectmanager.scheduling.repository;

import com.example.aiprojectmanager.scheduling.domain.TaskDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TaskDependencyRepository extends JpaRepository<TaskDependency, Long> {

    /** All dependency edges for a given project. */
    List<TaskDependency> findAllByProjectId(Long projectId);

    /** Check for an existing edge between two specific tasks. */
    Optional<TaskDependency> findByPredecessorTaskIdAndSuccessorTaskId(
            Long predecessorTaskId, Long successorTaskId);

    /** Remove all dependencies for a project (used in cascade scenarios). */
    void deleteAllByProjectId(Long projectId);

    /** Remove all dependencies involving a specific task (used in cascade scenarios). */
    void deleteByPredecessorTaskIdOrSuccessorTaskId(Long predecessorTaskId, Long successorTaskId);
}
