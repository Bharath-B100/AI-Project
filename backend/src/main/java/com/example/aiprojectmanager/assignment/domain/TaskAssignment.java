package com.example.aiprojectmanager.assignment.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.example.aiprojectmanager.task.domain.Task;
import com.example.aiprojectmanager.team.domain.TeamMember;

@Entity
@Table(name = "task_assignments")
@Getter
@Setter
@NoArgsConstructor
public class TaskAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_member_id", nullable = false)
    private TeamMember teamMember;

    @Column(name = "allocation_percentage", precision = 5, scale = 2, nullable = false)
    private BigDecimal allocationPercentage = BigDecimal.valueOf(100.0);

    @Column(name = "planned_hours", precision = 8, scale = 2)
    private BigDecimal plannedHours;

    @Column(name = "actual_hours", precision = 8, scale = 2)
    private BigDecimal actualHours = BigDecimal.ZERO;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void create() {
        assignedAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void update() {
        updatedAt = LocalDateTime.now();
    }
}
