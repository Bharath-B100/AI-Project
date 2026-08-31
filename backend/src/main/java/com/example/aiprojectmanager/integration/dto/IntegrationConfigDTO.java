package com.example.aiprojectmanager.integration.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/** Public DTO for integration configuration (never exposes raw tokens). */
@Data
@Builder
public class IntegrationConfigDTO {
    private Long          id;
    private Long          projectId;
    private String        provider;
    private String        displayName;
    private String        baseUrl;
    private String        status;
    private LocalDateTime lastSyncedAt;
    private boolean       tokenPresent;
    private String        configJson;
}
