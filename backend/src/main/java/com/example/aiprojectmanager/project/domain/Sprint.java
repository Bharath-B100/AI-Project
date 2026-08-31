package com.example.aiprojectmanager.project.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * An Agile sprint within a project.
 * Tasks are linked via {@code Task.sprintId} (nullable — null = backlog).
 */
@Entity
@Table(name = "sprints")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Sprint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String goal;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    /** PLANNED | ACTIVE | COMPLETED */
    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "PLANNED";

    @Builder.Default
    @Column(name = "velocity_points")
    private Integer velocityPoints = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}
