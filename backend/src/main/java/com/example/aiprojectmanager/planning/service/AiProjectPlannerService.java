package com.example.aiprojectmanager.planning.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AiProjectPlannerService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final TaskDependencyRepository dependencyRepository;
    private final GanttDataService ganttDataService;

    /**
     * Decomposes a natural language prompt into a structured project plan (WBS).
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

        // Determine project domain and title
        String projectName = extractProjectName(prompt, lower);
        String description = "AI-generated project plan for: " + prompt;

        List<GeneratedMilestoneDto> milestones = new ArrayList<>();
        List<GeneratedTaskDto> tasks = new ArrayList<>();
        List<String> roles = new ArrayList<>();

        if (lower.contains("mobile") || lower.contains("ios") || lower.contains("android") || lower.contains("app")) {
            buildMobileAppPlan(months, milestones, tasks, roles);
        } else if (lower.contains("e-commerce") || lower.contains("ecommerce") || lower.contains("shop") || lower.contains("store")) {
            buildEcommercePlan(months, milestones, tasks, roles);
        } else if (lower.contains("ai") || lower.contains("ml") || lower.contains("machine learning") || lower.contains("llm") || lower.contains("model")) {
            buildAiMlPlan(months, milestones, tasks, roles);
        } else if (lower.contains("cloud") || lower.contains("migration") || lower.contains("devops") || lower.contains("infra")) {
            buildCloudMigrationPlan(months, milestones, tasks, roles);
        } else {
            buildSaaSWebPlan(projectName, months, milestones, tasks, roles);
        }

        // Calculate total estimated days and recommended budget
        int totalDays = tasks.stream().mapToInt(GeneratedTaskDto::getDurationDays).sum();
        int compressedDays = Math.max(months * 22, (int) (totalDays * 0.45)); // With parallel critical path
        
        BigDecimal totalHours = tasks.stream()
                .map(t -> t.getEstimatedHours() != null ? t.getEstimatedHours() : BigDecimal.valueOf(30))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal calculatedBudget = request.getBudget() != null && request.getBudget().compareTo(BigDecimal.ZERO) > 0
                ? request.getBudget()
                : totalHours.multiply(BigDecimal.valueOf(500)); // Default standard labor baseline

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
     * Commits the AI generated plan to the database atomically and triggers CPM calculation.
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

        // 2. Create Tasks & record tempId -> real Task ID mapping
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

        // 3. Build Task Dependency edges
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

        // 4. Calculate initial CPM Schedule
        ScheduleCalculationResponse schedule = null;
        try {
            schedule = ganttDataService.calculateSchedule(savedProject.getId());
        } catch (Exception ignored) {
            // If CPM fails due to loose graph, fallback gracefully
        }

        Map<String, Object> response = new HashMap<>();
        response.put("projectId", savedProject.getId());
        response.put("projectName", savedProject.getName());
        response.put("taskCount", savedTasks.size());
        response.put("dependencyCount", dependencies.size());
        response.put("schedule", schedule);

        return response;
    }

    private String extractProjectName(String prompt, String lower) {
        if (prompt.length() <= 35) {
            return prompt;
        }
        if (lower.contains("mobile")) return "Mobile App Development";
        if (lower.contains("e-commerce") || lower.contains("ecommerce")) return "E-Commerce Digital Platform";
        if (lower.contains("ai") || lower.contains("ml")) return "AI / ML Analytics Engine";
        if (lower.contains("cloud") || lower.contains("migration")) return "Cloud Infrastructure Modernization";
        return "NextGen Web Application";
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
                .description("Create store listings, prepare release screenshots, configure release signing keys, and submit builds.")
                .estimatedHours(BigDecimal.valueOf(20)).durationDays(4).priority("HIGH")
                .milestone("Phase 4").requiredSkills(List.of("App Store Connect", "Play Console", "CI/CD"))
                .dependsOnTempIds(List.of("T7")).build());
    }

    private void buildEcommercePlan(int months, List<GeneratedMilestoneDto> milestones, List<GeneratedTaskDto> tasks, List<String> roles) {
        roles.addAll(List.of("Full Stack Lead", "Frontend Specialist", "Payment / Backend Engineer", "Database Specialist", "QA Tester"));

        milestones.add(new GeneratedMilestoneDto("Phase 1: Catalog & Product Modeling", "Product taxonomy, inventory schemas, search & filter engine.", 20));
        milestones.add(new GeneratedMilestoneDto("Phase 2: Shopping Cart & Checkout Funnel", "Cart state, discounts, taxes, address validation.", 45));
        milestones.add(new GeneratedMilestoneDto("Phase 3: Payment Gateway & Order Processing", "Stripe/Razorpay integration, webhooks, invoice generation.", 70));
        milestones.add(new GeneratedMilestoneDto("Phase 4: Admin Dashboard & Go-Live", "Analytics, order fulfillment, security hardening, production launch.", 90));

        tasks.add(GeneratedTaskDto.builder().tempId("T1").title("Product Catalog Data Architecture & API")
                .description("Model product variations, categories, inventory stock levels, and indexing.")
                .estimatedHours(BigDecimal.valueOf(36)).durationDays(6).priority("HIGH")
                .milestone("Phase 1").requiredSkills(List.of("PostgreSQL", "Database Design")).build());

        tasks.add(GeneratedTaskDto.builder().tempId("T2").title("Storefront UI & Product Browsing Experience")
                .description("Build responsive product grid, multi-attribute filter panel, and high-res image gallery.")
                .estimatedHours(BigDecimal.valueOf(40)).durationDays(7).priority("HIGH")
                .milestone("Phase 1").requiredSkills(List.of("React", "CSS", "UI Design")).build());

        tasks.add(GeneratedTaskDto.builder().tempId("T3").title("Shopping Cart & Real-Time Stock Reservation")
                .description("Implement persistent shopping cart with concurrency lock for inventory reservation.")
                .estimatedHours(BigDecimal.valueOf(32)).durationDays(5).priority("HIGH")
                .milestone("Phase 2").requiredSkills(List.of("Redis", "Backend API"))
                .dependsOnTempIds(List.of("T1", "T2")).build());

        tasks.add(GeneratedTaskDto.builder().tempId("T4").title("Multi-Gateway Payment Integration (Stripe / UPI)")
                .description("Secure PCI-compliant checkout workflow with idempotency keys and asynchronous webhook listeners.")
                .estimatedHours(BigDecimal.valueOf(44)).durationDays(8).priority("HIGH")
                .milestone("Phase 3").requiredSkills(List.of("Payments", "Security", "Webhooks"))
                .dependsOnTempIds(List.of("T3")).build());

        tasks.add(GeneratedTaskDto.builder().tempId("T5").title("Order Fulfillment & Invoice Notification System")
                .description("Automated order confirmation emails, PDF tax invoice generation, and status tracking.")
                .estimatedHours(BigDecimal.valueOf(28)).durationDays(5).priority("MEDIUM")
                .milestone("Phase 3").requiredSkills(List.of("Java", "Email Service", "PDF Generation"))
                .dependsOnTempIds(List.of("T4")).build());

        tasks.add(GeneratedTaskDto.builder().tempId("T6").title("Merchant Analytics & Admin Control Panel")
                .description("Sales velocity dashboard, customer management, inventory alerts, and revenue reports.")
                .estimatedHours(BigDecimal.valueOf(35)).durationDays(6).priority("MEDIUM")
                .milestone("Phase 4").requiredSkills(List.of("React", "Charts", "Admin UI"))
                .dependsOnTempIds(List.of("T5")).build());

        tasks.add(GeneratedTaskDto.builder().tempId("T7").title("Load Testing, Security Penetration & Launch")
                .description("Execute black-friday scale load tests, rate-limiting verification, and CDN optimization.")
                .estimatedHours(BigDecimal.valueOf(24)).durationDays(4).priority("HIGH")
                .milestone("Phase 4").requiredSkills(List.of("DevOps", "Performance", "Security"))
                .dependsOnTempIds(List.of("T6")).build());
    }

    private void buildAiMlPlan(int months, List<GeneratedMilestoneDto> milestones, List<GeneratedTaskDto> tasks, List<String> roles) {
        roles.addAll(List.of("AI/ML Research Engineer", "MLOps / Data Engineer", "Full Stack Developer", "Domain Analyst"));

        milestones.add(new GeneratedMilestoneDto("Phase 1: Data Pipeline & Preprocessing", "ETL pipelines, data cleaning, validation, and feature store.", 20));
        milestones.add(new GeneratedMilestoneDto("Phase 2: Model Architecture & Training", "Baseline modeling, hyperparameter tuning, evaluation metrics.", 50));
        milestones.add(new GeneratedMilestoneDto("Phase 3: Model Serving & Real-Time API", "Triton/FastAPI deployment, low-latency inference, caching.", 70));
        milestones.add(new GeneratedMilestoneDto("Phase 4: Dashboard Integration & MLOps Monitoring", "Drift detection, automated retraining, interactive UI.", 90));

        tasks.add(GeneratedTaskDto.builder().tempId("T1").title("Data Ingestion & Feature Engineering Pipeline")
                .description("Build automated ETL scripts to ingest raw data, remove anomalies, and generate normalized features.")
                .estimatedHours(BigDecimal.valueOf(40)).durationDays(8).priority("HIGH")
                .milestone("Phase 1").requiredSkills(List.of("Python", "Pandas", "ETL")).build());

        tasks.add(GeneratedTaskDto.builder().tempId("T2").title("Baseline Model Training & Benchmark Validation")
                .description("Train candidate models, evaluate Precision/Recall/F1, and perform cross-validation.")
                .estimatedHours(BigDecimal.valueOf(48)).durationDays(10).priority("HIGH")
                .milestone("Phase 2").requiredSkills(List.of("PyTorch", "Scikit-Learn", "Modeling"))
                .dependsOnTempIds(List.of("T1")).build());

        tasks.add(GeneratedTaskDto.builder().tempId("T3").title("Inference Optimization & Model Serialization")
                .description("Quantize model weights (ONNX/TensorRT) for sub-50ms inference latency.")
                .estimatedHours(BigDecimal.valueOf(32)).durationDays(6).priority("MEDIUM")
                .milestone("Phase 3").requiredSkills(List.of("ONNX", "Optimization"))
                .dependsOnTempIds(List.of("T2")).build());

        tasks.add(GeneratedTaskDto.builder().tempId("T4").title("High-Throughput Model Serving Microservice")
                .description("Develop asynchronous REST/gRPC API with batching, health probes, and Redis prediction caching.")
                .estimatedHours(BigDecimal.valueOf(36)).durationDays(7).priority("HIGH")
                .milestone("Phase 3").requiredSkills(List.of("FastAPI", "Docker", "Redis"))
                .dependsOnTempIds(List.of("T3")).build());

        tasks.add(GeneratedTaskDto.builder().tempId("T5").title("Interactive AI Analytics Dashboard")
                .description("Build visual frontend for real-time model predictions, explainability charts, and confidence scores.")
                .estimatedHours(BigDecimal.valueOf(36)).durationDays(7).priority("MEDIUM")
                .milestone("Phase 4").requiredSkills(List.of("React", "D3.js", "Analytics"))
                .dependsOnTempIds(List.of("T4")).build());

        tasks.add(GeneratedTaskDto.builder().tempId("T6").title("MLOps Monitoring & Data Drift Guardrails")
                .description("Deploy Prometheus metrics for prediction drift, latency anomalies, and automated alert triggers.")
                .estimatedHours(BigDecimal.valueOf(24)).durationDays(5).priority("HIGH")
                .milestone("Phase 4").requiredSkills(List.of("MLOps", "Prometheus", "Grafana"))
                .dependsOnTempIds(List.of("T5")).build());
    }

    private void buildCloudMigrationPlan(int months, List<GeneratedMilestoneDto> milestones, List<GeneratedTaskDto> tasks, List<String> roles) {
        roles.addAll(List.of("Cloud Solutions Architect", "DevOps Engineer", "Database Administrator", "Security Specialist"));

        milestones.add(new GeneratedMilestoneDto("Phase 1: Discovery & Cloud Architecture", "Infrastructure audit, target cloud VPC design, IaC setup.", 15));
        milestones.add(new GeneratedMilestoneDto("Phase 2: Database & Storage Migration", "Zero-downtime database replication, blob storage sync.", 40));
        milestones.add(new GeneratedMilestoneDto("Phase 3: Containerization & Kubernetes", "Dockerization, Helm charts, CI/CD pipeline automation.", 65));
        milestones.add(new GeneratedMilestoneDto("Phase 4: Cutover & Performance Hardening", "DNS traffic switch, rollback strategy, WAF & observability.", 90));

        tasks.add(GeneratedTaskDto.builder().tempId("T1").title("Terraform Infrastructure as Code (IaC) Provisioning")
                .description("Define VPC, subnets, security groups, managed Kubernetes cluster, and IAM policies.")
                .estimatedHours(BigDecimal.valueOf(36)).durationDays(6).priority("HIGH")
                .milestone("Phase 1").requiredSkills(List.of("Terraform", "AWS/GCP", "Networking")).build());

        tasks.add(GeneratedTaskDto.builder().tempId("T2").title("Managed Database Replication Setup")
                .description("Configure CDC (Change Data Capture) database replication with automated fallback failover.")
                .estimatedHours(BigDecimal.valueOf(40)).durationDays(7).priority("HIGH")
                .milestone("Phase 2").requiredSkills(List.of("PostgreSQL", "Database Migration"))
                .dependsOnTempIds(List.of("T1")).build());

        tasks.add(GeneratedTaskDto.builder().tempId("T3").title("Application Containerization & Helm Packaging")
                .description("Create multi-stage Docker builds, Kubernetes manifests, resource limits, and secrets management.")
                .estimatedHours(BigDecimal.valueOf(32)).durationDays(6).priority("HIGH")
                .milestone("Phase 3").requiredSkills(List.of("Docker", "Kubernetes", "Helm"))
                .dependsOnTempIds(List.of("T1")).build());

        tasks.add(GeneratedTaskDto.builder().tempId("T4").title("Automated CI/CD GitOps Deployment Pipeline")
                .description("Set up GitHub Actions / ArgoCD workflow for zero-downtime rolling canary deployments.")
                .estimatedHours(BigDecimal.valueOf(28)).durationDays(5).priority("HIGH")
                .milestone("Phase 3").requiredSkills(List.of("CI/CD", "GitHub Actions", "ArgoCD"))
                .dependsOnTempIds(List.of("T3")).build());

        tasks.add(GeneratedTaskDto.builder().tempId("T5").title("Dry-Run Staging Cutover & Stress Testing")
                .description("Execute simulated traffic failover, benchmark latency, and verify database integrity.")
                .estimatedHours(BigDecimal.valueOf(30)).durationDays(5).priority("HIGH")
                .milestone("Phase 4").requiredSkills(List.of("Performance", "Testing"))
                .dependsOnTempIds(List.of("T2", "T4")).build());

        tasks.add(GeneratedTaskDto.builder().tempId("T6").title("Final Production DNS Switch & Observability")
                .description("Update Route53/Cloudflare DNS records, verify SSL certificates, and monitor Datadog alerts.")
                .estimatedHours(BigDecimal.valueOf(16)).durationDays(3).priority("HIGH")
                .milestone("Phase 4").requiredSkills(List.of("DNS", "Monitoring", "Production Operations"))
                .dependsOnTempIds(List.of("T5")).build());
    }

    private void buildSaaSWebPlan(String projectName, int months, List<GeneratedMilestoneDto> milestones, List<GeneratedTaskDto> tasks, List<String> roles) {
        roles.addAll(List.of("Tech Lead / Architect", "Full Stack Developer", "Frontend Engineer", "QA Engineer"));

        milestones.add(new GeneratedMilestoneDto("Phase 1: Architecture & UI Prototype", "Product specifications, Figma design system, API contracts.", 15));
        milestones.add(new GeneratedMilestoneDto("Phase 2: Core Platform & Security", "User accounts, tenant isolation, database models, CRUD APIs.", 40));
        milestones.add(new GeneratedMilestoneDto("Phase 3: Business Logic & Integrations", "Domain workflows, real-time events, reporting, exports.", 65));
        milestones.add(new GeneratedMilestoneDto("Phase 4: QA, Hardening & Launch", "Security audits, performance tuning, production release.", 90));

        tasks.add(GeneratedTaskDto.builder().tempId("T1").title("System Architecture & UI/UX Design System")
                .description("Design high-fidelity UI components, database ER diagram, and REST API specification.")
                .estimatedHours(BigDecimal.valueOf(36)).durationDays(6).priority("HIGH")
                .milestone("Phase 1").requiredSkills(List.of("UI/UX", "Architecture", "Figma")).build());

        tasks.add(GeneratedTaskDto.builder().tempId("T2").title("Data Models, Migrations & Core Backend API")
                .description("Implement database schema, Flyway migrations, JPA repositories, and service layer.")
                .estimatedHours(BigDecimal.valueOf(40)).durationDays(7).priority("HIGH")
                .milestone("Phase 2").requiredSkills(List.of("Java", "Spring Boot", "PostgreSQL"))
                .dependsOnTempIds(List.of("T1")).build());

        tasks.add(GeneratedTaskDto.builder().tempId("T3").title("Authentication, RBAC & Multi-Tenant Security")
                .description("Implement JWT stateless authentication, password hashing, and role-based permissions.")
                .estimatedHours(BigDecimal.valueOf(28)).durationDays(5).priority("HIGH")
                .milestone("Phase 2").requiredSkills(List.of("Spring Security", "JWT"))
                .dependsOnTempIds(List.of("T2")).build());

        tasks.add(GeneratedTaskDto.builder().tempId("T4").title("Interactive Responsive Web UI Development")
                .description("Build dashboard, data tables, modals, form validations, and state synchronization.")
                .estimatedHours(BigDecimal.valueOf(44)).durationDays(8).priority("HIGH")
                .milestone("Phase 3").requiredSkills(List.of("React", "TypeScript", "Tailwind/CSS"))
                .dependsOnTempIds(List.of("T1", "T3")).build());

        tasks.add(GeneratedTaskDto.builder().tempId("T5").title("Business Logic & Automated Analytics Engine")
                .description("Implement domain business calculation algorithms, CSV/PDF export, and status tracking.")
                .estimatedHours(BigDecimal.valueOf(36)).durationDays(6).priority("HIGH")
                .milestone("Phase 3").requiredSkills(List.of("Java", "Analytics", "Business Logic"))
                .dependsOnTempIds(List.of("T4")).build());

        tasks.add(GeneratedTaskDto.builder().tempId("T6").title("Comprehensive QA Testing & Security Auditing")
                .description("Write unit tests, execute integration scenarios, and verify input sanitization.")
                .estimatedHours(BigDecimal.valueOf(24)).durationDays(5).priority("MEDIUM")
                .milestone("Phase 4").requiredSkills(List.of("JUnit", "QA", "Security Testing"))
                .dependsOnTempIds(List.of("T5")).build());

        tasks.add(GeneratedTaskDto.builder().tempId("T7").title("CI/CD Pipeline & Production Deployment")
                .description("Automate Docker container build, health monitoring probes, and production deployment.")
                .estimatedHours(BigDecimal.valueOf(16)).durationDays(3).priority("HIGH")
                .milestone("Phase 4").requiredSkills(List.of("Docker", "CI/CD", "DevOps"))
                .dependsOnTempIds(List.of("T6")).build());
    }
}
