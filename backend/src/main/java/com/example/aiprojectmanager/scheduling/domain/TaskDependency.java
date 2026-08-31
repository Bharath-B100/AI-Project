package com.example.aiprojectmanager.scheduling.domain;

import com.example.aiprojectmanager.task.domain.DependencyType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Represents a directed dependency edge between two tasks in the same project.
 * predecessor → successor with a configurable dependency type and optional lag.
 */
@Entity
@Table(
    name = "task_dependencies",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_td_pair",
        columnNames = {"predecessor_task_id", "successor_task_id"}
    )
)
@Getter @Setter @NoArgsConstructor
public class TaskDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "predecessor_task_id", nullable = false)
    private Long predecessorTaskId;

    @Column(name = "successor_task_id", nullable = false)
    private Long successorTaskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dependency_type", nullable = false, length = 30)
    private DependencyType dependencyType = DependencyType.FINISH_TO_START;

    @Column(name = "lag_days", nullable = false)
    private Integer lagDays = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
