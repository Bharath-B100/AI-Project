package com.example.aiprojectmanager.task.repository;
import com.example.aiprojectmanager.task.domain.Task; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface TaskRepository extends JpaRepository<Task,Long> { List<Task> findAllByProjectIdOrderByDueDateAsc(Long projectId); List<Task> findByProjectId(Long projectId); List<Task> findBySprintId(Long sprintId); List<Task> findBySprintIdAndProjectId(Long sprintId, Long projectId); void deleteByProjectId(Long projectId); }

