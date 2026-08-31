package com.example.aiprojectmanager.risk.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.example.aiprojectmanager.project.domain.Project;

@Entity
@Table(name = "project_risks")
@Getter
@Setter
@NoArgsConstructor
public class ProjectRisk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_type", nullable = false, length = 50)
    private RiskType riskType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RiskSeverity severity;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "evidence_json", columnDefinition = "TEXT")
    private String evidenceJson;

    @Column(name = "suggested_action", columnDefinition = "TEXT")
    private String suggestedAction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RiskStatus status = RiskStatus.OPEN;

    @Column(name = "detected_at", nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void create() {
        detectedAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void update() {
        updatedAt = LocalDateTime.now();
    }
}
