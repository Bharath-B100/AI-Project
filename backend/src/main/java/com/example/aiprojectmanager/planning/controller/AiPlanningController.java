package com.example.aiprojectmanager.planning.controller;

import com.example.aiprojectmanager.planning.dto.CommitPlanRequest;
import com.example.aiprojectmanager.planning.dto.GeneratedPlanDto;
import com.example.aiprojectmanager.planning.dto.PlanGenerationRequest;
import com.example.aiprojectmanager.planning.service.AiProjectPlannerService;
import com.example.aiprojectmanager.user.domain.User;
import com.example.aiprojectmanager.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/planning")
@RequiredArgsConstructor
public class AiPlanningController {

    private final AiProjectPlannerService plannerService;
    private final UserRepository userRepository;

    private Long getUserId(Authentication auth) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        return user.getId();
    }

    /**
     * POST /api/v1/planning/generate
     * Decomposes natural language prompt into full WBS, milestones, tasks, and dependencies.
     */
    @PostMapping("/generate")
    public ResponseEntity<GeneratedPlanDto> generatePlan(@Valid @RequestBody PlanGenerationRequest request) {
        GeneratedPlanDto plan = plannerService.generatePlan(request);
        return ResponseEntity.ok(plan);
    }

    /**
     * POST /api/v1/planning/commit
     * Atomically creates Project, Tasks, TaskDependencies, and calculates initial CPM schedule.
     */
    @PostMapping("/commit")
    public ResponseEntity<Map<String, Object>> commitPlan(
            Authentication auth,
            @Valid @RequestBody CommitPlanRequest request) {
        Long ownerId = getUserId(auth);
        Map<String, Object> result = plannerService.commitPlan(request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
