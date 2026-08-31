package com.example.aiprojectmanager.integration.dto;

import lombok.Data;

/** Request body for connecting a new integration. */
@Data
public class ConnectIntegrationRequest {
    private String provider;      // JIRA | ASANA | MONDAY | GITHUB | GITLAB | MS_PROJECT
    private String displayName;
    private String baseUrl;
    private String accessToken;
    private String refreshToken;
    private String configJson;    // e.g. {"repoOwner":"acme","repoName":"my-app"}
}
