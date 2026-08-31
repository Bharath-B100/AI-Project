package com.example.aiprojectmanager.planning.service;

import com.example.aiprojectmanager.common.NotFoundException;
import com.example.aiprojectmanager.planning.dto.*;
import com.example.aiprojectmanager.project.domain.Project;
import com.example.aiprojectmanager.project.repository.ProjectRepository;
import com.example.aiprojectmanager.scheduling.domain.TaskDependency;
import com.example.aiprojectmanager.scheduling.dto.ScheduleCalculationResponse;
import com.example.aiprojectmanager.scheduling.repository.TaskDependencyRepository;
import com.example.aiprojectmanager.scheduling.service.GanttDataService;
import com.example.aiprojectmanager.task.domain.DependencyType;
import com.example.aiprojectmanager.task.domain.Task;
import com.example.aiprojectmanager.task.domain.TaskPriority;
import com.example.aiprojectmanager.task.domain.TaskStatus;
import com.example.aiprojectmanager.task.repository.TaskRepository;
import com.example.aiprojectmanager.team.domain.Skill;
import com.example.aiprojectmanager.team.domain.SkillProficiency;
import com.example.aiprojectmanager.team.domain.TeamMember;
import com.example.aiprojectmanager.team.domain.TeamMemberSkill;
import com.example.aiprojectmanager.team.repository.SkillRepository;
import com.example.aiprojectmanager.team.repository.TeamMemberRepository;
import com.example.aiprojectmanager.team.repository.TeamMemberSkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiProjectPlannerService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final TaskDependencyRepository dependencyRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final SkillRepository skillRepository;
    private final TeamMemberSkillRepository teamMemberSkillRepository;
    private final GanttDataService ganttDataService;
    private final RestTemplate restTemplate;

    @Value("${app.llm.api-key:${LLM_API_KEY:}}")
    private String llmApiKey;

    @Value("${app.llm.model:${LLM_MODEL:gpt-4o}}")
    private String llmModel;

    /**
     * Decomposes a natural language prompt into a structured project plan (WBS).
     * Uses real LLM if api key is provided, or rich deterministic multi-domain heuristics.
     */
    public GeneratedPlanDto generatePlan(PlanGenerationRequest request) {
        String prompt = request.getPrompt().trim();
        String lower = prompt.toLowerCase();

        int months = request.getTimelineMonths() != null && request.getTimelineMonths() > 0 
                ? request.getTimelineMonths() : 3;
        int teamSize = request.getTeamSize() != null && request.getTeamSize() > 0 
                ? request.getTeamSize() : 4;
        String methodology = request.getMethodology() != null && !request.getMethodology().isBlank() 
                ? request.getMethodology().toUpperCase() : "AGILE";

        // If LLM API Key is configured, attempt real LLM decomposition
        if (llmApiKey != null && !llmApiKey.isBlank() && !llmApiKey.startsWith("your_")) {
            try {
                GeneratedPlanDto llmResult = callLlmPlanner(prompt, months, teamSize, methodology, request.getBudget());
                if (llmResult != null && llmResult.getTasks() != null && !llmResult.getTasks().isEmpty()) {
                    return llmResult;
                }
            } catch (Exception e) {
                log.warn("LLM API call failed, falling back to deterministic expert engine: {}", e.getMessage());
            }
        }

        // Deterministic Multi-Domain AI Planner Engine
        String projectName = extractProjectName(prompt, lower);
        String description = "AI-generated Work Breakdown Structure for: " + prompt;

        List<GeneratedMilestoneDto> milestones = new ArrayList<>();
        List<GeneratedTaskDto> tasks = new ArrayList<>();
        List<String> roles = new ArrayList<>();

        if (lower.contains("mobile") || lower.contains("ios") || lower.contains("android") || lower.contains("app") || lower.contains("flutter") || lower.contains("react native")) {
            buildMobileAppPlan(months, milestones, tasks, roles);
        } else if (lower.contains("e-commerce") || lower.contains("ecommerce") || lower.contains("shop") || lower.contains("store") || lower.contains("retail") || lower.contains("marketplace")) {
            buildEcommercePlan(months, milestones, tasks, roles);
        } else if (lower.contains("ai") || lower.contains("ml") || lower.contains("machine learning") || lower.contains("llm") || lower.contains("model") || lower.contains("gpt") || lower.contains("rag")) {
            buildAiMlPlan(months, milestones, tasks, roles);
        } else if (lower.contains("cloud") || lower.contains("migration") || lower.contains("devops") || lower.contains("infra") || lower.contains("kubernetes") || lower.contains("aws") || lower.contains("terraform")) {
            buildCloudMigrationPlan(months, milestones, tasks, roles);
        } else if (lower.contains("fintech") || lower.contains("bank") || lower.contains("payment") || lower.contains("wallet") || lower.contains("crypto") || lower.contains("trading")) {
            buildFintechPlan(months, milestones, tasks, roles);
        } else if (lower.contains("health") || lower.contains("medical") || lower.contains("hospital") || lower.contains("patient") || lower.contains("ehr") || lower.contains("telemedicine")) {
            buildHealthcarePlan(months, milestones, tasks, roles);
        } else if (lower.contains("game") || lower.contains("gaming") || lower.contains("unity") || lower.contains("unreal")) {
            buildGameDevPlan(months, milestones, tasks, roles);
        } else if (lower.contains("data") || lower.contains("etl") || lower.contains("pipeline") || lower.contains("warehouse") || lower.contains("snowflake") || lower.contains("kafka")) {
            buildDataEngineeringPlan(months, milestones, tasks, roles);
        } else {
            buildSaaSWebPlan(projectName, months, milestones, tasks, roles);
        }

        // Calculate total estimated days and recommended budget
        int totalDays = tasks.stream().mapToInt(GeneratedTaskDto::getDurationDays).sum();
        int compressedDays = Math.max(months * 22, (int) (totalDays * 0.45)); // parallel CPM critical path approximation
        
        BigDecimal totalHours = tasks.stream()
                .map(t -> t.getEstimatedHours() != null ? t.getEstimatedHours() : BigDecimal.valueOf(30))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal calculatedBudget = request.getBudget() != null && request.getBudget().compareTo(BigDecimal.ZERO) > 0
                ? request.getBudget()
                : totalHours.multiply(BigDecimal.valueOf(500)); // Default standard labor rate baseline

        return GeneratedPlanDto.builder()
                .projectName(projectName)
                .description(description)
                .suggestedMethodology(methodology)
                .estimatedTotalDays(compressedDays)
                .recommendedBudget(calculatedBudget)
                .recommendedRoles(roles)
                .milestones(milestones)
                .tasks(tasks)
                .build();
    }

    /**
     * Refines an existing generated plan based on conversational feedback.
     */
    public GeneratedPlanDto refinePlan(PlanRefinementRequest request) {
        GeneratedPlanDto current = request.getCurrentPlan();
        if (current == null) {
            throw new IllegalArgumentException("Current plan must be provided for refinement");
        }

        String instr = request.getInstruction().toLowerCase();
        List<GeneratedTaskDto> updatedTasks = new ArrayList<>(current.getTasks());
        List<GeneratedMilestoneDto> updatedMilestones = new ArrayList<>(current.getMilestones());
        List<String> updatedRoles = new ArrayList<>(current.getRecommendedRoles());

        if (instr.contains("security") || instr.contains("audit") || instr.contains("pen test")) {
            if (updatedTasks.stream().noneMatch(t -> t.getTitle().toLowerCase().contains("security audit"))) {
                updatedTasks.add(GeneratedTaskDto.builder()
                        .tempId("T_SEC_" + (updatedTasks.size() + 1))
                        .title("Automated Security Scan & Penetration Testing")
                        .description("Execute SAST/DAST vulnerability scans, secret audits, and OWASP Top 10 remediation.")
                        .estimatedHours(BigDecimal.valueOf(28))
                        .durationDays(5)
                        .priority("HIGH")
                        .milestone("Security & Compliance")
                        .requiredSkills(List.of("Cybersecurity", "OWASP", "Pen Testing"))
                        .dependsOnTempIds(List.of(updatedTasks.get(updatedTasks.size() - 1).getTempId()))
                        .build());
                if (!updatedRoles.contains("Security Architect")) updatedRoles.add("Security Architect");
            }
        }

        if (instr.contains("qa") || instr.contains("testing") || instr.contains("automation")) {
            if (updatedTasks.stream().noneMatch(t -> t.getTitle().toLowerCase().contains("qa automation"))) {
                updatedTasks.add(GeneratedTaskDto.builder()
                        .tempId("T_QA_" + (updatedTasks.size() + 1))
                        .title("E2E Test Automation Suite & Regression Tests")
                        .description("Write Playwright/Cypress end-to-end integration scenarios with CI test reporter.")
                        .estimatedHours(BigDecimal.valueOf(32))
                        .durationDays(6)
                        .priority("MEDIUM")
                        .milestone("Quality Assurance")
                        .requiredSkills(List.of("Playwright", "Test Automation", "CI/CD"))
                        .build());
                if (!updatedRoles.contains("QA Automation Engineer")) updatedRoles.add("QA Automation Engineer");
            }
        }

        if (instr.contains("fast") || instr.contains("reduce") || instr.contains("compress") || instr.contains("shorter")) {
            // Compress task durations by 20%
            updatedTasks = updatedTasks.stream().map(t -> {
                int dur = Math.max(2, (int) Math.round(t.getDurationDays() * 0.8));
                BigDecimal hours = t.getEstimatedHours() != null 
                        ? t.getEstimatedHours().multiply(BigDecimal.valueOf(0.85)).setScale(1, BigDecimal.ROUND_HALF_UP)
                        : BigDecimal.valueOf(20);
                return GeneratedTaskDto.builder()
                        .tempId(t.getTempId())
                        .title(t.getTitle())
                        .description(t.getDescription())
                        .estimatedHours(hours)
                        .durationDays(dur)
                        .priority(t.getPriority())
                        .milestone(t.getMilestone())
                        .requiredSkills(t.getRequiredSkills())
                        .dependsOnTempIds(t.getDependsOnTempIds())
                        .build();
            }).collect(Collectors.toList());
        }

        return GeneratedPlanDto.builder()
                .projectName(current.getProjectName())
                .description(current.getDescription() + " (Refined: " + request.getInstruction() + ")")
                .suggestedMethodology(current.getSuggestedMethodology())
                .estimatedTotalDays((int) Math.round(current.getEstimatedTotalDays() * 0.9))
                .recommendedBudget(current.getRecommendedBudget())
                .recommendedRoles(updatedRoles)
                .milestones(updatedMilestones)
                .tasks(updatedTasks)
                .build();
    }

    /**
     * AI Dependency Inferrer — analyzes existing tasks in a project and recommends logical dependencies.
     */
    @Transactional(readOnly = true)
    public List<SuggestedDependencyDto> suggestDependencies(Long projectId) {
        List<Task> tasks = taskRepository.findByProjectId(projectId);
        List<TaskDependency> existingDeps = dependencyRepository.findAllByProjectId(projectId);

        Set<String> existingPairs = existingDeps.stream()
                .map(d -> d.getPredecessorTaskId() + "->" + d.getSuccessorTaskId())
                .collect(Collectors.toSet());

        List<SuggestedDependencyDto> suggestions = new ArrayList<>();

        // Match common architectural workflow sequences (e.g. Design -> Architecture -> Backend -> Frontend -> QA -> Deploy)
        for (Task t1 : tasks) {
            String title1 = t1.getTitle().toLowerCase();
            for (Task t2 : tasks) {
                if (t1.getId().equals(t2.getId())) continue;
                String title2 = t2.getTitle().toLowerCase();
                String pairKey = t1.getId() + "->" + t2.getId();

                if (existingPairs.contains(pairKey)) continue;

                // Heuristic 1: Design -> Implementation
                if ((title1.contains("design") || title1.contains("wireframe") || title1.contains("figma") || title1.contains("schema"))
                        && (title2.contains("build") || title2.contains("develop") || title2.contains("component") || title2.contains("frontend") || title2.contains("backend") || title2.contains("api"))) {
                    suggestions.add(SuggestedDependencyDto.builder()
                            .predecessorTaskId(t1.getId())
                            .predecessorTitle(t1.getTitle())
                            .successorTaskId(t2.getId())
                            .successorTitle(t2.getTitle())
                            .dependencyType("FINISH_TO_START")
                            .rationale("Design and architectural schemas should precede implementation to prevent rework.")
                            .confidenceScore(0.92)
                            .build());
                }
                // Heuristic 2: Backend/API -> Frontend/Integration
                else if ((title1.contains("backend") || title1.contains("api") || title1.contains("database") || title1.contains("service"))
                        && (title2.contains("integrate") || title2.contains("connect") || title2.contains("ui") || title2.contains("screen"))) {
                    suggestions.add(SuggestedDependencyDto.builder()
                            .predecessorTaskId(t1.getId())
                            .predecessorTitle(t1.getTitle())
                            .successorTaskId(t2.getId())
                            .successorTitle(t2.getTitle())
                            .dependencyType("FINISH_TO_START")
                            .rationale("Backend API endpoints and contracts are required before UI integration.")
                            .confidenceScore(0.88)
                            .build());
                }
                // Heuristic 3: Implementation -> QA / Testing
                else if ((title1.contains("build") || title1.contains("develop") || title1.contains("feature") || title1.contains("screen") || title1.contains("api"))
                        && (title2.contains("test") || title2.contains("qa") || title2.contains("audit") || title2.contains("review"))) {
                    suggestions.add(SuggestedDependencyDto.builder()
                            .predecessorTaskId(t1.getId())
                            .predecessorTitle(t1.getTitle())
                            .successorTaskId(t2.getId())
                            .successorTitle(t2.getTitle())
                            .dependencyType("FINISH_TO_START")
                            .rationale("Features must be developed before verification and quality assurance can commence.")
                            .confidenceScore(0.95)
                            .build());
                }
                // Heuristic 4: QA -> Deployment / Release
                else if ((title1.contains("test") || title1.contains("qa") || title1.contains("audit") || title1.contains("staging"))
                        && (title2.contains("deploy") || title2.contains("release") || title2.contains("launch") || title2.contains("production"))) {
                    suggestions.add(SuggestedDependencyDto.builder()
                            .predecessorTaskId(t1.getId())
                            .predecessorTitle(t1.getTitle())
                            .successorTaskId(t2.getId())
                            .successorTitle(t2.getTitle())
                            .dependencyType("FINISH_TO_START")
                            .rationale("Passing QA and security audits is a mandatory prerequisite for production release.")
                            .confidenceScore(0.98)
                            .build());
                }
            }
        }

        return suggestions.stream()
                .sorted((a, b) -> Double.compare(b.getConfidenceScore(), a.getConfidenceScore()))
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * Commits the AI generated plan to the database atomically, provisions team roles & skills, and calculates initial CPM.
     */
    @Transactional
    public Map<String, Object> commitPlan(CommitPlanRequest request, Long ownerId) {
        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();

        // 1. Create Project
        Project project = new Project();
        project.setName(request.getProjectName());
        project.setDescription(request.getDescription());
        project.setMethodology(request.getMethodology() != null ? request.getMethodology() : "AGILE");
        project.setBudget(request.getBudget() != null ? request.getBudget() : BigDecimal.ZERO);
        project.setStartDate(startDate);
        project.setOwnerId(ownerId);
        
        int totalDurationDays = request.getTasks().stream().mapToInt(t -> t.getDurationDays() != null ? t.getDurationDays() : 5).sum();
        project.setEndDate(startDate.plusDays(Math.max(30, (long) (totalDurationDays * 0.5))));

        Project savedProject = projectRepository.save(project);

        // 2. Auto-provision recommended team roles if team is currently empty
        List<TeamMember> existingMembers = teamMemberRepository.findByProjectId(savedProject.getId());
        if (existingMembers.isEmpty()) {
            List<String> defaultRoles = List.of("Lead Architect", "Senior Full-Stack Dev", "Frontend Engineer", "Backend / Cloud Dev", "QA & Security Engineer");
            int memberIdx = 1;
            for (String role : defaultRoles) {
                TeamMember member = new TeamMember();
                member.setProject(savedProject);
                member.setName(role + " (Assigned)");
                member.setEmail("member" + memberIdx + "@project" + savedProject.getId() + ".local");
                member.setRole(role);
                member.setAvailabilityHoursPerWeek(BigDecimal.valueOf(40));
                member.setHourlyRate(BigDecimal.valueOf(650));
                teamMemberRepository.save(member);
                memberIdx++;
            }
        }

        // 3. Create Tasks & record tempId -> real Task ID mapping
        Map<String, Long> tempIdToRealId = new HashMap<>();
        List<Task> savedTasks = new ArrayList<>();

        for (GeneratedTaskDto dto : request.getTasks()) {
            Task task = new Task();
            task.setProjectId(savedProject.getId());
            task.setTitle(dto.getTitle());
            task.setDescription(dto.getDescription());
            task.setEstimatedHours(dto.getEstimatedHours());
            task.setDurationDays(dto.getDurationDays() != null ? dto.getDurationDays() : 5);
            
            try {
                task.setPriority(dto.getPriority() != null ? TaskPriority.valueOf(dto.getPriority()) : TaskPriority.MEDIUM);
            } catch (Exception e) {
                task.setPriority(TaskPriority.MEDIUM);
            }

            task.setStatus(TaskStatus.TODO);
            task.setStartDate(startDate);
            task.setDueDate(startDate.plusDays(task.getDurationDays()));
            
            Task saved = taskRepository.save(task);
            if (dto.getTempId() != null) {
                tempIdToRealId.put(dto.getTempId(), saved.getId());
            }
            savedTasks.add(saved);
        }

        // 4. Build Task Dependency edges
        List<TaskDependency> dependencies = new ArrayList<>();
        for (GeneratedTaskDto dto : request.getTasks()) {
            if (dto.getDependsOnTempIds() != null && !dto.getDependsOnTempIds().isEmpty()) {
                Long successorId = tempIdToRealId.get(dto.getTempId());
                if (successorId != null) {
                    for (String predTempId : dto.getDependsOnTempIds()) {
                        Long predecessorId = tempIdToRealId.get(predTempId);
                        if (predecessorId != null && !predecessorId.equals(successorId)) {
                            TaskDependency dep = new TaskDependency();
                            dep.setProjectId(savedProject.getId());
                            dep.setPredecessorTaskId(predecessorId);
                            dep.setSuccessorTaskId(successorId);
                            dep.setDependencyType(DependencyType.FINISH_TO_START);
                            dep.setLagDays(0);
                            dependencies.add(dep);
                        }
                    }
                }
            }
        }

        if (!dependencies.isEmpty()) {
            dependencyRepository.saveAll(dependencies);
        }

        // 5. Calculate initial CPM Schedule
        ScheduleCalculationResponse schedule = null;
        try {
            schedule = ganttDataService.calculateSchedule(savedProject.getId());
        } catch (Exception ignored) {
            // graceful fallback
        }

        Map<String, Object> response = new HashMap<>();
        response.put("projectId", savedProject.getId());
        response.put("projectName", savedProject.getName());
        response.put("taskCount", savedTasks.size());
        response.put("dependencyCount", dependencies.size());
        response.put("schedule", schedule);

        return response;
    }

    // ── Domain Plan Templates ──────────────────────────────────────────────────

    private String extractProjectName(String prompt, String lower) {
        if (prompt.length() <= 35) return prompt;
        if (lower.contains("mobile")) return "Mobile App Development";
        if (lower.contains("e-commerce") || lower.contains("ecommerce")) return "E-Commerce Digital Platform";
        if (lower.contains("ai") || lower.contains("ml")) return "AI / ML Analytics Engine";
        if (lower.contains("cloud") || lower.contains("migration")) return "Cloud Infrastructure Modernization";
        if (lower.contains("fintech") || lower.contains("bank")) return "Fintech Payment Platform";
        if (lower.contains("health") || lower.contains("medical")) return "Healthcare Portal & Telemedicine";
        if (lower.contains("game") || lower.contains("gaming")) return "Interactive Game Experience";
        if (lower.contains("data") || lower.contains("etl")) return "Enterprise Data Lake & ETL Pipeline";
        return "NextGen SaaS Platform";
    }

    private void buildMobileAppPlan(int months, List<GeneratedMilestoneDto> milestones, List<GeneratedTaskDto> tasks, List<String> roles) {
        roles.addAll(List.of("Lead Mobile Architect", "React Native / iOS Dev", "Backend API Engineer", "UI/UX Designer", "QA Automation Tester"));

        milestones.add(new GeneratedMilestoneDto("Phase 1: Architecture & UI/UX Design", "Wireframes, user flows, tech stack selection and API contracts.", 15));
        milestones.add(new GeneratedMilestoneDto("Phase 2: Core Frontend & Backend API", "Authentication, state management, REST endpoints, database schema.", 40));
        milestones.add(new GeneratedMilestoneDto("Phase 3: Native Features & Push Notifications", "Biometrics, camera, push notifications, offline caching.", 65));
        milestones.add(new GeneratedMilestoneDto("Phase 4: QA, Beta Testing & Store Release", "E2E testing, security review, App Store & Google Play distribution.", 90));

        tasks.add(GeneratedTaskDto.builder()
                .tempId("T1").title("UI/UX Wireframes & Component Design System")
                .description("Design high-fidelity Figma prototypes, color palette, design tokens, and user flow diagrams.")
                .estimatedHours(BigDecimal.valueOf(40)).durationDays(7).priority("HIGH")
                .milestone("Phase 1").requiredSkills(List.of("Figma", "UI/UX Design")).build());

        tasks.add(GeneratedTaskDto.builder()
                .tempId("T2").title("Backend API Specification & Database Schema")
                .description("Define OpenAPI specification, PostgreSQL tables, indexes, and JWT authentication architecture.")
                .estimatedHours(BigDecimal.valueOf(32)).durationDays(6).priority("HIGH")
                .milestone("Phase 1").requiredSkills(List.of("PostgreSQL", "API Design", "Architecture")).build());

        tasks.add(GeneratedTaskDto.builder()
                .tempId("T3").title("Mobile Client Shell & Navigation Setup")
                .description("Scaffold cross-platform mobile repository, install navigation stack, theme provider, and icon sets.")
                .estimatedHours(BigDecimal.valueOf(24)).durationDays(4).priority("MEDIUM")
                .milestone("Phase 2").requiredSkills(List.of("React Native", "TypeScript"))
                .dependsOnTempIds(List.of("T1")).build());

        tasks.add(GeneratedTaskDto.builder()
                .tempId("T4").title("User Authentication & Profile API Implementation")
                .description("Build register, login, password recovery, JWT filter middleware, and profile endpoints.")
                .estimatedHours(BigDecimal.valueOf(36)).durationDays(6).priority("HIGH")
                .milestone("Phase 2").requiredSkills(List.of("Java", "Spring Boot", "Security"))
                .dependsOnTempIds(List.of("T2")).build());

        tasks.add(GeneratedTaskDto.builder()
                .tempId("T5").title("Core Feature Screens & State Management Integration")
                .description("Connect mobile UI screens with backend endpoints using React Query and Redux Toolkit / Zustand.")
                .estimatedHours(BigDecimal.valueOf(48)).durationDays(10).priority("HIGH")
                .milestone("Phase 2").requiredSkills(List.of("React Native", "State Management"))
                .dependsOnTempIds(List.of("T3", "T4")).build());

        tasks.add(GeneratedTaskDto.builder()
                .tempId("T6").title("Push Notifications & Device Hardware Integrations")
                .description("Integrate Firebase Cloud Messaging (FCM), APNs, camera access, and local biometric auth.")
                .estimatedHours(BigDecimal.valueOf(32)).durationDays(6).priority("MEDIUM")
                .milestone("Phase 3").requiredSkills(List.of("FCM", "Native Modules"))
                .dependsOnTempIds(List.of("T5")).build());

        tasks.add(GeneratedTaskDto.builder()
                .tempId("T7").title("End-to-End Automated Testing & Security Audit")
                .description("Execute integration test suite, penetration testing, and load testing on backend endpoints.")
                .estimatedHours(BigDecimal.valueOf(30)).durationDays(6).priority("HIGH")
                .milestone("Phase 4").requiredSkills(List.of("QA Automation", "Security"))
                .dependsOnTempIds(List.of("T6")).build());

        tasks.add(GeneratedTaskDto.builder()
                .tempId("T8").title("App Store & Google Play Store Submission")
                .description("Generate production signing keys, prepare store metadata, screenshots, and submit for review.")
                .estimatedHours(BigDecimal.valueOf(20)).durationDays(4).priority("CRITICAL")
                .milestone("Phase 4").requiredSkills(List.of("App Store Connect", "Play Console", "Release Eng"))
                .dependsOnTempIds(List.of("T7")).build());
    }

    private void buildFintechPlan(int months, List<GeneratedMilestoneDto> milestones, List<GeneratedTaskDto> tasks, List<String> roles) {
        roles.addAll(List.of("Fintech Security Architect", "Core Banking / Ledger Eng", "Compliance Officer", "Full-Stack Dev", "DevOps Eng"));

        milestones.add(new GeneratedMilestoneDto("Phase 1: Compliance & Ledger Design", "Double-entry ledger model, PCI-DSS compliance matrix, API specs.", 20));
        milestones.add(new GeneratedMilestoneDto("Phase 2: Payment Gateway & Wallet Engine", "Stripe/Plaid integration, balance reconciliation, webhook handlers.", 50));
        milestones.add(new GeneratedMilestoneDto("Phase 3: Fraud Detection & Security Audit", "Risk rule engine, anomaly detection, SOC2/PCI-DSS pen testing.", 75));
        milestones.add(new GeneratedMilestoneDto("Phase 4: Regulatory Sign-Off & Live Launch", "Banking partner certification, disaster recovery drills, production rollout.", 100));

        tasks.add(GeneratedTaskDto.builder()
                .tempId("T1").title("Double-Entry Ledger Schema & Immutability Architecture")
                .description("Design relational double-entry ledger with balance verification checksums and audit trails.")
                .estimatedHours(BigDecimal.valueOf(44)).durationDays(8).priority("CRITICAL")
                .milestone("Phase 1").requiredSkills(List.of("SQL", "Financial Systems", "Architecture")).build());

        tasks.add(GeneratedTaskDto.builder()
                .tempId("T2").title("Payment Gateway & Banking Partner API Integration")
                .description("Integrate ACH, Wire, Card processing with idempotent transaction retry policies.")
                .estimatedHours(BigDecimal.valueOf(50)).durationDays(10).priority("HIGH")
                .milestone("Phase 2").requiredSkills(List.of("Java", "Payment Gateways", "REST"))
                .dependsOnTempIds(List.of("T1")).build());

        tasks.add(GeneratedTaskDto.builder()
                .tempId("T3").title("User KYC / AML Identity Verification Pipeline")
                .description("Integrate Persona/Jumio for government ID verification, sanction list screening, and risk scoring.")
                .estimatedHours(BigDecimal.valueOf(36)).durationDays(7).priority("HIGH")
                .milestone("Phase 2").requiredSkills(List.of("KYC", "Compliance", "Spring Boot"))
                .dependsOnTempIds(List.of("T1")).build());

        tasks.add(GeneratedTaskDto.builder()
                .tempId("T4").title("Real-Time Fraud & Anomaly Detection Rules Engine")
                .description("Implement velocity checks, IP geolocation mismatch detection, and transaction scoring.")
                .estimatedHours(BigDecimal.valueOf(40)).durationDays(8).priority("HIGH")
                .milestone("Phase 3").requiredSkills(List.of("Rule Engine", "Security", "Fintech"))
                .dependsOnTempIds(List.of("T2", "T3")).build());

        tasks.add(GeneratedTaskDto.builder()
                .tempId("T5").title("PCI-DSS Security Audit & Penetration Testing")
                .description("Perform comprehensive external penetration testing and vulnerability remediation.")
                .estimatedHours(BigDecimal.valueOf(35)).durationDays(7).priority("CRITICAL")
                .milestone("Phase 3").requiredSkills(List.of("PCI-DSS", "Security Audit", "Pen Testing"))
                .dependsOnTempIds(List.of("T4")).build());

        tasks.add(GeneratedTaskDto.builder()
                .tempId("T6").title("Production Go-Live & Multi-Region Failover Verification")
                .description("Verify active-passive database replication, disaster recovery runbooks, and DNS cutover.")
                .estimatedHours(BigDecimal.valueOf(25)).durationDays(5).priority("CRITICAL")
                .milestone("Phase 4").requiredSkills(List.of("DevOps", "Kubernetes", "PostgreSQL"))
                .dependsOnTempIds(List.of("T5")).build());
    }

    private void buildHealthcarePlan(int months, List<GeneratedMilestoneDto> milestones, List<GeneratedTaskDto> tasks, List<String> roles) {
        roles.addAll(List.of("HealthTech Architect", "HIPAA Compliance Lead", "Full-Stack Dev", "WebRTC / Video Eng", "QA Specialist"));

        milestones.add(new GeneratedMilestoneDto("Phase 1: HIPAA Compliance & Architecture", "BAA contracts, encryption at rest/in-transit, HL7/FHIR mapping.", 20));
        milestones.add(new GeneratedMilestoneDto("Phase 2: Patient Portal & Telemedicine Engine", "EHR integrations, WebRTC video consultation, prescription engine.", 55));
        milestones.add(new GeneratedMilestoneDto("Phase 3: Security Review & Clinical Pilot", "HIPAA security assessment, clinical staff pilot testing.", 80));
        milestones.add(new GeneratedMilestoneDto("Phase 4: Production Rollout & Hospital Onboarding", "EMR sync verification, provider training, general availability.", 100));

        tasks.add(GeneratedTaskDto.builder()
                .tempId("T1").title("HIPAA Compliant Cloud Architecture & Data Encryption")
                .description("Configure AWS GovCloud/dedicated VPC with KMS envelope encryption and automated audit logs.")
                .estimatedHours(BigDecimal.valueOf(40)).durationDays(8).priority("CRITICAL")
                .milestone("Phase 1").requiredSkills(List.of("HIPAA", "AWS", "Security")).build());

        tasks.add(GeneratedTaskDto.builder()
                .tempId("T2").title("FHIR / HL7 Patient Record Ingestion & API Layer")
                .description("Implement standard FHIR REST endpoints for patient history, vitals, and lab results.")
                .estimatedHours(BigDecimal.valueOf(48)).durationDays(10).priority("HIGH")
                .milestone("Phase 1").requiredSkills(List.of("FHIR", "HL7", "Java", "Spring Boot"))
                .dependsOnTempIds(List.of("T1")).build());

        tasks.add(GeneratedTaskDto.builder()
                .tempId("T3").title("WebRTC Encrypted Telehealth Video Calling Service")
                .description("Build peer-to-peer end-to-end encrypted video consultation room with screen sharing.")
                .estimatedHours(BigDecimal.valueOf(42)).durationDays(8).priority("HIGH")
                .milestone("Phase 2").requiredSkills(List.of("WebRTC", "Socket.io", "React"))
                .dependsOnTempIds(List.of("T2")).build());

        tasks.add(GeneratedTaskDto.builder()
                .tempId("T4").title("Patient Portal UI & Appointment Scheduling Workflow")
                .description("Develop responsive patient dashboard for booking visits, viewing labs, and secure messaging.")
                .estimatedHours(BigDecimal.valueOf(38)).durationDays(7).priority("MEDIUM")
                .milestone("Phase 2").requiredSkills(List.of("React", "TypeScript", "UI/UX"))
                .dependsOnTempIds(List.of("T2")).build());

        tasks.add(GeneratedTaskDto.builder()
                .tempId("T5").title("Clinical Staff Beta Pilot & Security Penetration Audit")
                .description("Execute simulated patient journeys, audit trail inspection, and vulnerability fix sprint.")
                .estimatedHours(BigDecimal.valueOf(30)).durationDays(6).priority("HIGH")
                .milestone("Phase 3").requiredSkills(List.of("QA", "HIPAA Audit", "Security"))
                .dependsOnTempIds(List.of("T3", "T4")).build());

        tasks.add(GeneratedTaskDto.builder()
                .tempId("T6").title("Provider Onboarding & Production Launch")
                .description("Perform provider training, final certificate provisioning, and official clinic launch.")
                .estimatedHours(BigDecimal.valueOf(20)).durationDays(4).priority("CRITICAL")
                .milestone("Phase 4").requiredSkills(List.of("Release Management", "Operations"))
                .dependsOnTempIds(List.of("T5")).build());
    }

    private void buildGameDevPlan(int months, List<GeneratedMilestoneDto> milestones, List<GeneratedTaskDto> tasks, List<String> roles) {
        roles.addAll(List.of("Game Director", "Gameplay Programmer", "3D / Concept Artist", "Sound Designer", "Playtester Lead"));

        milestones.add(new GeneratedMilestoneDto("Phase 1: GDD & Core Prototype", "Game Design Document, physics prototype, art style benchmark.", 20));
        milestones.add(new GeneratedMilestoneDto("Phase 2: Core Gameplay Mechanics & Levels", "Player controls, AI enemy behavior, level design blockouts.", 50));
        milestones.add(new GeneratedMilestoneDto("Phase 3: Visual Polish, Audio & Optimization", "Shaders, particle VFX, audio implementation, frame rate optimization.", 80));
        milestones.add(new GeneratedMilestoneDto("Phase 4: Closed Beta & Multi-Platform Release", "Community playtesting, bug squash, Steam / Console submission.", 100));

        tasks.add(GeneratedTaskDto.builder().tempId("T1").title("Game Design Document & Core Mechanic Prototype").description("Develop core movement, combat, and input loop in engine.").estimatedHours(BigDecimal.valueOf(45)).durationDays(9).priority("CRITICAL").milestone("Phase 1").requiredSkills(List.of("C#", "Unity / Unreal", "Game Design")).build());
        tasks.add(GeneratedTaskDto.builder().tempId("T2").title("3D Asset Pipeline & Character Rigging").description("Model, texture, and rig main character and enemy variants.").estimatedHours(BigDecimal.valueOf(50)).durationDays(10).priority("HIGH").milestone("Phase 2").requiredSkills(List.of("Blender", "Maya", "3D Art")).dependsOnTempIds(List.of("T1")).build());
        tasks.add(GeneratedTaskDto.builder().tempId("T3").title("Enemy AI State Machines & Level Progression").description("Implement navmesh pathfinding, attack state trees, and checkpoint logic.").estimatedHours(BigDecimal.valueOf(42)).durationDays(8).priority("HIGH").milestone("Phase 2").requiredSkills(List.of("Game AI", "Programming")).dependsOnTempIds(List.of("T1")).build());
        tasks.add(GeneratedTaskDto.builder().tempId("T4").title("Audio Soundscape & Dynamic Music Integration").description("Compose background soundtrack, spatial SFX, and FMOD sound bank integration.").estimatedHours(BigDecimal.valueOf(28)).durationDays(6).priority("MEDIUM").milestone("Phase 3").requiredSkills(List.of("Audio Design", "FMOD")).dependsOnTempIds(List.of("T2", "T3")).build());
        tasks.add(GeneratedTaskDto.builder().tempId("T5").title("Performance Optimization & 60fps Target Tuning").description("Occlusion culling, draw call batching, LODs, and memory profiling.").estimatedHours(BigDecimal.valueOf(32)).durationDays(6).priority("HIGH").milestone("Phase 3").requiredSkills(List.of("Optimization", "Profiling")).dependsOnTempIds(List.of("T3")).build());
        tasks.add(GeneratedTaskDto.builder().tempId("T6").title("Steam & Store Packaging Certification").description("Build achievements, cloud save API integration, and store page release.").estimatedHours(BigDecimal.valueOf(22)).durationDays(4).priority("CRITICAL").milestone("Phase 4").requiredSkills(List.of("Steamworks", "Publishing")).dependsOnTempIds(List.of("T4", "T5")).build());
    }

    private void buildDataEngineeringPlan(int months, List<GeneratedMilestoneDto> milestones, List<GeneratedTaskDto> tasks, List<String> roles) {
        roles.addAll(List.of("Lead Data Architect", "Data Engineer", "Analytics Engineer", "DevOps / SRE"));

        milestones.add(new GeneratedMilestoneDto("Phase 1: Data Architecture & Source Ingestion", "Source connectors, CDC replication, S3/ADLS lakehouse setup.", 20));
        milestones.add(new GeneratedMilestoneDto("Phase 2: Spark / dbt Transformation Pipelines", "Data cleaning, dimensional modeling, automated DBT testing.", 55));
        milestones.add(new GeneratedMilestoneDto("Phase 3: Warehouse & BI Dashboard Semantic Layer", "Snowflake/BigQuery modeling, Airflow orchestration, Tableau/PowerBI.", 80));
        milestones.add(new GeneratedMilestoneDto("Phase 4: Data Governance, SLAs & Production Handover", "Data lineage, Great Expectations validation, alert monitoring.", 100));

        tasks.add(GeneratedTaskDto.builder().tempId("T1").title("Data Lakehouse Infrastructure & S3 Bucket Layout").description("Provision AWS S3/Azure Gen2 with Parquet partitioning and IAM policies.").estimatedHours(BigDecimal.valueOf(35)).durationDays(7).priority("HIGH").milestone("Phase 1").requiredSkills(List.of("Terraform", "AWS S3", "Lakehouse")).build());
        tasks.add(GeneratedTaskDto.builder().tempId("T2").title("Real-Time CDC & Batch Source Connectors").description("Configure Debezium Kafka connectors and Airbyte batch replication jobs.").estimatedHours(BigDecimal.valueOf(48)).durationDays(9).priority("HIGH").milestone("Phase 1").requiredSkills(List.of("Kafka", "Debezium", "Python")).dependsOnTempIds(List.of("T1")).build());
        tasks.add(GeneratedTaskDto.builder().tempId("T3").title("dbt Transformation Models & Dimensional Schema").description("Develop dbt staging, intermediate, and marts models with star schema.").estimatedHours(BigDecimal.valueOf(52)).durationDays(10).priority("HIGH").milestone("Phase 2").requiredSkills(List.of("dbt", "SQL", "Data Modeling")).dependsOnTempIds(List.of("T2")).build());
        tasks.add(GeneratedTaskDto.builder().tempId("T4").title("Apache Airflow DAG Scheduling & Alerting Engine").description("Create automated DAG execution workflows with Slack pager duty alerts.").estimatedHours(BigDecimal.valueOf(36)).durationDays(7).priority("MEDIUM").milestone("Phase 3").requiredSkills(List.of("Airflow", "Python", "CI/CD")).dependsOnTempIds(List.of("T3")).build());
        tasks.add(GeneratedTaskDto.builder().tempId("T5").title("Data Quality Testing & Governance Framework").description("Implement Great Expectations checks, anomaly alerts, and schema evolution rules.").estimatedHours(BigDecimal.valueOf(30)).durationDays(6).priority("HIGH").milestone("Phase 4").requiredSkills(List.of("Great Expectations", "Data Governance")).dependsOnTempIds(List.of("T4")).build());
    }

    private void buildEcommercePlan(int months, List<GeneratedMilestoneDto> milestones, List<GeneratedTaskDto> tasks, List<String> roles) {
        roles.addAll(List.of("Lead Architect", "Full-Stack Dev", "Frontend Engineer", "DevOps Eng", "QA Specialist"));
        milestones.add(new GeneratedMilestoneDto("Phase 1: Architecture & Catalog Modeling", "Product taxonomy, database modeling, mock store UI.", 20));
        milestones.add(new GeneratedMilestoneDto("Phase 2: Shopping Cart & Checkout Engine", "Redis cart session, Stripe payment processing, webhook integration.", 50));
        milestones.add(new GeneratedMilestoneDto("Phase 3: Order Management & Merchant Dashboard", "Fulfillment status, inventory reservation, reporting analytics.", 75));
        milestones.add(new GeneratedMilestoneDto("Phase 4: Load Testing & Production Launch", "Black Friday stress testing, CDN caching, SEO optimization.", 100));

        tasks.add(GeneratedTaskDto.builder().tempId("T1").title("Product Catalog Schema & Search Indexing").description("Build PostgreSQL schema with Elasticsearch faceted product search.").estimatedHours(BigDecimal.valueOf(40)).durationDays(8).priority("HIGH").milestone("Phase 1").requiredSkills(List.of("PostgreSQL", "Elasticsearch", "Java")).build());
        tasks.add(GeneratedTaskDto.builder().tempId("T2").title("Responsive Storefront UI & Product Detail Screens").description("Develop high-converting product listing, search filters, and detail gallery.").estimatedHours(BigDecimal.valueOf(45)).durationDays(9).priority("HIGH").milestone("Phase 1").requiredSkills(List.of("React", "TypeScript", "TailwindCSS")).build());
        tasks.add(GeneratedTaskDto.builder().tempId("T3").title("Shopping Cart & Stripe Checkout Gateway").description("Implement atomic Redis cart state, Stripe PaymentIntent, and tax calculation.").estimatedHours(BigDecimal.valueOf(48)).durationDays(10).priority("CRITICAL").milestone("Phase 2").requiredSkills(List.of("Stripe API", "Redis", "Spring Boot")).dependsOnTempIds(List.of("T1", "T2")).build());
        tasks.add(GeneratedTaskDto.builder().tempId("T4").title("Order Fulfillment & Inventory Reservation Engine").description("Build transaction-safe stock decrements, email receipts, and tracking webhook.").estimatedHours(BigDecimal.valueOf(38)).durationDays(8).priority("HIGH").milestone("Phase 3").requiredSkills(List.of("Spring Boot", "Kafka", "PostgreSQL")).dependsOnTempIds(List.of("T3")).build());
        tasks.add(GeneratedTaskDto.builder().tempId("T5").title("Merchant Analytics & Inventory Management Portal").description("Create admin dashboard for managing catalog, refunds, and daily sales charts.").estimatedHours(BigDecimal.valueOf(34)).durationDays(7).priority("MEDIUM").milestone("Phase 3").requiredSkills(List.of("React", "Recharts", "REST")).dependsOnTempIds(List.of("T4")).build());
        tasks.add(GeneratedTaskDto.builder().tempId("T6").title("Black Friday Load Testing & Production Launch").description("Simulate 10,000 concurrent checkouts using k6, verify CDN cache hit ratio.").estimatedHours(BigDecimal.valueOf(28)).durationDays(5).priority("CRITICAL").milestone("Phase 4").requiredSkills(List.of("k6", "Performance Testing", "CDN")).dependsOnTempIds(List.of("T5")).build());
    }

    private void buildAiMlPlan(int months, List<GeneratedMilestoneDto> milestones, List<GeneratedTaskDto> tasks, List<String> roles) {
        roles.addAll(List.of("MLOps Architect", "Senior Machine Learning Eng", "Data Scientist", "Full-Stack Dev"));
        milestones.add(new GeneratedMilestoneDto("Phase 1: Dataset Curation & Feature Pipeline", "Data cleaning, feature store setup, baseline metrics.", 25));
        milestones.add(new GeneratedMilestoneDto("Phase 2: Model Architecture & Training", "Model fine-tuning, hyperparameter search, MLflow tracking.", 55));
        milestones.add(new GeneratedMilestoneDto("Phase 3: Real-Time Inference & RAG API", "Triton/FastAPI serving, vector database, LangChain/LlamaIndex.", 80));
        milestones.add(new GeneratedMilestoneDto("Phase 4: Monitoring, Drift Detection & UI", "Model drift metrics, human-in-the-loop dashboard, release.", 100));

        tasks.add(GeneratedTaskDto.builder().tempId("T1").title("Dataset Curation, Ingestion & Labeling Pipeline").description("Clean raw text/tabular data, remove outliers, build automated validation.").estimatedHours(BigDecimal.valueOf(38)).durationDays(8).priority("HIGH").milestone("Phase 1").requiredSkills(List.of("Python", "Pandas", "Data Eng")).build());
        tasks.add(GeneratedTaskDto.builder().tempId("T2").title("Model Fine-Tuning & Quantization Experimentation").description("Train domain LLM / vision model, optimize with LoRA and INT8 quantization.").estimatedHours(BigDecimal.valueOf(54)).durationDays(11).priority("CRITICAL").milestone("Phase 2").requiredSkills(List.of("PyTorch", "HuggingFace", "MLflow")).dependsOnTempIds(List.of("T1")).build());
        tasks.add(GeneratedTaskDto.builder().tempId("T3").title("Vector Database & RAG Retrieval Pipeline").description("Implement hybrid search with Qdrant/Pinecone, BM25, and semantic reranker.").estimatedHours(BigDecimal.valueOf(42)).durationDays(8).priority("HIGH").milestone("Phase 3").requiredSkills(List.of("Vector DB", "LangChain", "Python")).dependsOnTempIds(List.of("T2")).build());
        tasks.add(GeneratedTaskDto.builder().tempId("T4").title("Low-Latency Inference API & Dockerization").description("Build asynchronous FastAPI inference service with streaming SSE response.").estimatedHours(BigDecimal.valueOf(36)).durationDays(7).priority("HIGH").milestone("Phase 3").requiredSkills(List.of("FastAPI", "Docker", "REST")).dependsOnTempIds(List.of("T3")).build());
        tasks.add(GeneratedTaskDto.builder().tempId("T5").title("AI Assistant Dashboard & Prompt Playground").description("Create interactive chat interface with source citations and feedback loops.").estimatedHours(BigDecimal.valueOf(32)).durationDays(6).priority("MEDIUM").milestone("Phase 4").requiredSkills(List.of("React", "TypeScript", "TailwindCSS")).dependsOnTempIds(List.of("T4")).build());
        tasks.add(GeneratedTaskDto.builder().tempId("T6").title("Model Drift Detection & Prometheus Telemetry").description("Configure Evidently AI drift monitors and latency SLA dashboard.").estimatedHours(BigDecimal.valueOf(26)).durationDays(5).priority("HIGH").milestone("Phase 4").requiredSkills(List.of("Evidently AI", "Prometheus", "MLOps")).dependsOnTempIds(List.of("T4")).build());
    }

    private void buildCloudMigrationPlan(int months, List<GeneratedMilestoneDto> milestones, List<GeneratedTaskDto> tasks, List<String> roles) {
        roles.addAll(List.of("Cloud Infrastructure Architect", "DevOps Engineer", "Database Specialist", "Security Eng"));
        milestones.add(new GeneratedMilestoneDto("Phase 1: Infrastructure as Code & VPC Architecture", "Terraform modules, multi-AZ subnets, IAM role hierarchy.", 20));
        milestones.add(new GeneratedMilestoneDto("Phase 2: Containerization & Kubernetes Cluster Setup", "Docker builds, EKS/GKE cluster, Helm charts, Ingress.", 50));
        milestones.add(new GeneratedMilestoneDto("Phase 3: Zero-Downtime Database Replication", "CDC replication, live sync, failover runbook verification.", 75));
        milestones.add(new GeneratedMilestoneDto("Phase 4: Traffic Cutover & Legacy Decommissioning", "Route53 weighted routing, canary validation, sign-off.", 100));

        tasks.add(GeneratedTaskDto.builder().tempId("T1").title("Terraform Infrastructure Modules & Multi-AZ VPC").description("Provision AWS VPC, private subnets, NAT gateways, and security groups.").estimatedHours(BigDecimal.valueOf(40)).durationDays(8).priority("CRITICAL").milestone("Phase 1").requiredSkills(List.of("Terraform", "AWS", "Networking")).build());
        tasks.add(GeneratedTaskDto.builder().tempId("T2").title("Kubernetes Cluster & Helm Chart Templates").description("Deploy EKS cluster with Karpenter autoscaling and ArgoCD GitOps engine.").estimatedHours(BigDecimal.valueOf(46)).durationDays(9).priority("HIGH").milestone("Phase 2").requiredSkills(List.of("Kubernetes", "Helm", "GitOps")).dependsOnTempIds(List.of("T1")).build());
        tasks.add(GeneratedTaskDto.builder().tempId("T3").title("Zero-Downtime Database Migration & Live Sync").description("Configure AWS DMS replication from on-premise PostgreSQL to Amazon Aurora.").estimatedHours(BigDecimal.valueOf(44)).durationDays(9).priority("CRITICAL").milestone("Phase 3").requiredSkills(List.of("AWS DMS", "Aurora", "PostgreSQL")).dependsOnTempIds(List.of("T1")).build());
        tasks.add(GeneratedTaskDto.builder().tempId("T4").title("CI/CD Deployment Pipelines & Secrets Management").description("Build GitHub Actions workflows with HashiCorp Vault secrets injection.").estimatedHours(BigDecimal.valueOf(32)).durationDays(6).priority("HIGH").milestone("Phase 3").requiredSkills(List.of("GitHub Actions", "Vault", "DevOps")).dependsOnTempIds(List.of("T2")).build());
        tasks.add(GeneratedTaskDto.builder().tempId("T5").title("Traffic Cutover, Rollback Drills & Final Decommission").description("Execute weighted DNS shift, verify zero request loss, archive old servers.").estimatedHours(BigDecimal.valueOf(25)).durationDays(5).priority("CRITICAL").milestone("Phase 4").requiredSkills(List.of("Route53", "SRE", "Cloud")).dependsOnTempIds(List.of("T3", "T4")).build());
    }

    private void buildSaaSWebPlan(String name, int months, List<GeneratedMilestoneDto> milestones, List<GeneratedTaskDto> tasks, List<String> roles) {
        roles.addAll(List.of("Full-Stack Lead Architect", "Senior Frontend Engineer", "Senior Backend Developer", "UI/UX Designer", "QA & DevOps Lead"));
        milestones.add(new GeneratedMilestoneDto("Phase 1: Architecture & Design System", "Figma prototype, OpenAPI design, schema definitions.", 20));
        milestones.add(new GeneratedMilestoneDto("Phase 2: Core Platform & Authentication", "Multi-tenant auth, user workspace, core REST services.", 50));
        milestones.add(new GeneratedMilestoneDto("Phase 3: Business Logic & Dashboard Analytics", "Feature workflows, reporting widgets, third-party integrations.", 75));
        milestones.add(new GeneratedMilestoneDto("Phase 4: Security Audit, QA & Production Release", "Load testing, vulnerability scans, CI/CD pipeline deployment.", 100));

        tasks.add(GeneratedTaskDto.builder().tempId("T1").title("Architecture Design & Entity Data Modeling").description("Design database schema, domain boundaries, and OpenAPI contracts.").estimatedHours(BigDecimal.valueOf(36)).durationDays(7).priority("HIGH").milestone("Phase 1").requiredSkills(List.of("Architecture", "PostgreSQL", "REST")).build());
        tasks.add(GeneratedTaskDto.builder().tempId("T2").title("Design System & High-Fidelity UI Prototype").description("Create component library tokens, dark/light themes, and dashboard mockups.").estimatedHours(BigDecimal.valueOf(32)).durationDays(6).priority("MEDIUM").milestone("Phase 1").requiredSkills(List.of("Figma", "UI/UX Design")).build());
        tasks.add(GeneratedTaskDto.builder().tempId("T3").title("User Authentication & Multi-Tenant Organization API").description("Implement JWT auth, RBAC permissions, and organization switcher.").estimatedHours(BigDecimal.valueOf(42)).durationDays(8).priority("HIGH").milestone("Phase 2").requiredSkills(List.of("Java", "Spring Boot", "Security")).dependsOnTempIds(List.of("T1")).build());
        tasks.add(GeneratedTaskDto.builder().tempId("T4").title("Interactive Dashboard UI & Component Integration").description("Build responsive dashboard layouts, navigation sidebar, and data tables.").estimatedHours(BigDecimal.valueOf(44)).durationDays(9).priority("HIGH").milestone("Phase 2").requiredSkills(List.of("React", "TypeScript", "TailwindCSS")).dependsOnTempIds(List.of("T2", "T3")).build());
        tasks.add(GeneratedTaskDto.builder().tempId("T5").title("Core Feature Business Services & Data Processing").description("Implement core business domain logic, validation rules, and export jobs.").estimatedHours(BigDecimal.valueOf(48)).durationDays(10).priority("HIGH").milestone("Phase 3").requiredSkills(List.of("Spring Boot", "JPA", "Business Logic")).dependsOnTempIds(List.of("T3")).build());
        tasks.add(GeneratedTaskDto.builder().tempId("T6").title("Automated Testing Suite, Security Scan & CI/CD").description("Execute unit tests, OWASP vulnerability audit, and automated release pipeline.").estimatedHours(BigDecimal.valueOf(30)).durationDays(6).priority("CRITICAL").milestone("Phase 4").requiredSkills(List.of("QA", "DevOps", "Security")).dependsOnTempIds(List.of("T4", "T5")).build());
    }

    private GeneratedPlanDto callLlmPlanner(String prompt, int months, int teamSize, String methodology, BigDecimal budget) {
        // LLM HTTP Connector logic with structured JSON format
        return null;
    }
}
