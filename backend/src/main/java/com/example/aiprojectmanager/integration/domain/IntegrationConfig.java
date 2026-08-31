package com.example.aiprojectmanager.integration.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores OAuth2 credentials and configuration for an external tool integration
 * (Jira, Asana, Monday.com, GitHub, GitLab, MS Project).
 */
@Entity
@Table(name = "integration_configs")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class IntegrationConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    /** JIRA | ASANA | MONDAY | GITHUB | GITLAB | MS_PROJECT */
    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "display_name", length = 150)
    private String displayName;

    @Column(name = "base_url", length = 500)
    private String baseUrl;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    /** JSON blob for provider-specific config (repo name, board ID, project key, etc.) */
    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    /** PENDING | CONNECTED | ERROR | DISCONNECTED */
    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}
