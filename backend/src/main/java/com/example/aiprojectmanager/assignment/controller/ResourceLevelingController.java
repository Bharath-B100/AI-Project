package com.example.aiprojectmanager.assignment.controller;

import com.example.aiprojectmanager.assignment.dto.LevelingReportDto;
import com.example.aiprojectmanager.assignment.service.ResourceLevelingService;
import com.example.aiprojectmanager.user.domain.User;
import com.example.aiprojectmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/projects/{projectId}/leveling")
@RequiredArgsConstructor
public class ResourceLevelingController {

    private final ResourceLevelingService levelingService;
    private final UserRepository userRepository;

    private Long getUserId(Authentication auth) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        return user.getId();
    }

    /**
     * GET /api/v1/projects/{projectId}/leveling/recommendations
     * Analyzes team allocation and suggests prescriptive task redistribution from overloaded members to available members.
     */
    @GetMapping("/recommendations")
    public ResponseEntity<LevelingReportDto> getRecommendations(
            @PathVariable Long projectId,
            Authentication auth) {
        Long ownerId = getUserId(auth);
        LevelingReportDto report = levelingService.calculateLevelingRecommendations(projectId, ownerId);
        return ResponseEntity.ok(report);
    }

    /**
     * POST /api/v1/projects/{projectId}/leveling/apply
     * 1-Click execution of recommended task reassignments.
     */
    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> applyLeveling(
            @PathVariable Long projectId,
            Authentication auth) {
        Long ownerId = getUserId(auth);
        Map<String, Object> result = levelingService.applyLevelingRecommendations(projectId, ownerId);
        return ResponseEntity.ok(result);
    }
}
