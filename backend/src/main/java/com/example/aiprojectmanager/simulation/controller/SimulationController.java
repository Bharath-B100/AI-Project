package com.example.aiprojectmanager.simulation.controller;

import com.example.aiprojectmanager.simulation.dto.SimulationRequest;
import com.example.aiprojectmanager.simulation.dto.SimulationResultDto;
import com.example.aiprojectmanager.simulation.service.SimulationService;
import com.example.aiprojectmanager.user.domain.User;
import com.example.aiprojectmanager.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/projects/{projectId}/simulate")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;
    private final UserRepository userRepository;

    private Long getUserId(Authentication auth) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        return user.getId();
    }

    /**
     * POST /api/v1/projects/{projectId}/simulate
     * Runs an in-memory CPM What-If scenario simulation with developer delta, productivity multiplier, and task duration overrides.
     */
    @PostMapping
    public ResponseEntity<SimulationResultDto> simulate(
            @PathVariable Long projectId,
            Authentication auth,
            @Valid @RequestBody(required = false) SimulationRequest request) {
        Long ownerId = getUserId(auth);
        SimulationRequest req = request != null ? request : new SimulationRequest();
        SimulationResultDto result = simulationService.simulateScenario(projectId, ownerId, req);
        return ResponseEntity.ok(result);
    }
}
