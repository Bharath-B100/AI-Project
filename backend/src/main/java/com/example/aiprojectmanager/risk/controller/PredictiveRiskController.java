package com.example.aiprojectmanager.risk.controller;

import com.example.aiprojectmanager.risk.dto.PredictiveRiskDto;
import com.example.aiprojectmanager.risk.service.PredictiveRiskService;
import com.example.aiprojectmanager.user.domain.User;
import com.example.aiprojectmanager.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/projects/{projectId}/risks/predictions")
@RequiredArgsConstructor
public class PredictiveRiskController {

    private final PredictiveRiskService predictiveRiskService;
    private final UserRepository userRepository;

    private Long getUserId(Authentication auth) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        return user.getId();
    }

    /**
     * GET /api/v1/projects/{projectId}/risks/predictions
     * Returns probabilistic delay prediction, Monte Carlo percentiles, and historical benchmark risk drivers.
     */
    @GetMapping
    public ResponseEntity<PredictiveRiskDto> getPredictiveRisk(
            @PathVariable Long projectId,
            Authentication auth) {
        Long ownerId = getUserId(auth);
        PredictiveRiskDto dto = predictiveRiskService.evaluatePredictiveRisk(projectId, ownerId);
        return ResponseEntity.ok(dto);
    }
}
