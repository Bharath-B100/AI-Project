package com.example.aiprojectmanager.integration.controller;

import com.example.aiprojectmanager.auth.CurrentUserService;
import com.example.aiprojectmanager.integration.dto.ConnectIntegrationRequest;
import com.example.aiprojectmanager.integration.dto.IntegrationConfigDTO;
import com.example.aiprojectmanager.integration.service.IntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Integration management REST endpoints.
 *
 * GET    /api/v1/projects/{id}/integrations                    — list integrations
 * POST   /api/v1/projects/{id}/integrations/connect            — connect a provider
 * POST   /api/v1/projects/{id}/integrations/{iid}/sync         — trigger sync
 * DELETE /api/v1/projects/{id}/integrations/{iid}              — disconnect
 */
@RestController
@RequestMapping("/projects/{projectId}/integrations")
@RequiredArgsConstructor
public class IntegrationController {

    private final IntegrationService  integrationService;
    private final CurrentUserService  currentUserService;

    @GetMapping
    public ResponseEntity<List<IntegrationConfigDTO>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(integrationService.listIntegrations(projectId, currentUserService.getCurrentUserId()));
    }

    @PostMapping("/connect")
    public ResponseEntity<IntegrationConfigDTO> connect(@PathVariable Long projectId,
                                                         @RequestBody ConnectIntegrationRequest req) {
        IntegrationConfigDTO dto = integrationService.connectIntegration(
                projectId, currentUserService.getCurrentUserId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/{integrationId}/sync")
    public ResponseEntity<Map<String, Object>> sync(@PathVariable Long projectId,
                                                     @PathVariable Long integrationId) {
        Map<String, Object> result = integrationService.syncIntegration(
                projectId, currentUserService.getCurrentUserId(), integrationId);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{integrationId}")
    public ResponseEntity<IntegrationConfigDTO> disconnect(@PathVariable Long projectId,
                                                            @PathVariable Long integrationId) {
        IntegrationConfigDTO dto = integrationService.disconnectIntegration(
                projectId, currentUserService.getCurrentUserId(), integrationId);
        return ResponseEntity.ok(dto);
    }
}
