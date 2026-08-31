# System Architecture

This document describes the high-level architecture of the AI Project Manager platform, explaining its modular monolithic design, backend module responsibilities, and system component layouts.

---

## Architectural Principles

The application is structured as a **Modular Monolith**. This approach ensures code isolation and maintainability while keeping the deployment simple.

* **Package by Feature**: Backend components are organized by functional area rather than technical layer. Each feature package (e.g., `project`, `task`) contains its own controllers, services, entities, repositories, DTOs, and module-specific validators.
* **Separation of Concerns**: Controllers deal strictly with HTTP requests/responses, request validation, and invoking service interfaces. Services contain business logic, transactional boundaries, and DTO mappings. Repositories handle database interactions.
* **Strict Communication Rules**:
  * Cross-module communication must happen through service interfaces, not directly via repositories or controllers of other modules.
  * Dependency injection is strictly controlled to avoid cyclic dependencies between modules.
  * Shared utilities, custom validation annotations, and global exception models live in the `common` package.

---

## Backend Modules

The backend contains the following modules under `com.aipm`:

1. **`auth`**: Handles authentication filter chain, token validation, user registration logic, password hashing, and Spring Security configurations.
2. **`user`**: Manages User accounts, profile retrieval, and role configuration.
3. **`project`**: Manages the lifecycle of a Project (creating, updating, archiving, budget limits, dashboard aggregation).
4. **`task`**: Manages Milestones, Tasks, Task Assignments, and Task Dependencies.
5. **`team`**: Handles Team Members, Skills mapping, working hour profiles, and project allocation assignments.
6. **`planning`**: Manages the staged AI generation requests (`AiGeneration` entity) before approval.
7. **`scheduling`**: The scheduling engine. Calculates project scheduling dates (using CPM logic) based on task durations and dependency records.
8. **`risk`**: The rules engine that runs calculations on workload, dates, and budget to detect and record project risks.
9. **`report`**: Handles creation and storage of status reports.
10. **`ai`**: Integrates with the LLM API using Spring AI clients. Handles prompt compiling, payload translation, and fallback mechanics.
11. **`common`**: Houses global exception handlers, pagination DTOs, security constants, and base utility classes.

---

## System Architecture Diagram

The Mermaid diagram below outlines the modular monolithic system architecture:

```mermaid
graph TD
    %% Clients
    Browser[React TypeScript SPA / Vite]

    %% Web Layer (Monolith Gateway / Controllers)
    subgraph Spring Boot Backend [Modular Monolith JVM Application]
        subgraph Web Layer
            AuthController[AuthController]
            ProjectController[ProjectController]
            TaskController[TaskController]
            TeamController[TeamController]
            PlanningController[PlanningController]
            ReportController[ReportController]
        end

        %% Service Modules
        subgraph Service Modules
            AuthService[AuthService]
            UserService[UserService]
            ProjectService[ProjectService]
            TaskService[TaskService]
            TeamService[TeamService]
            PlanningService[PlanningService]
            SchedulingService[SchedulingService]
            RiskService[RiskService]
            ReportService[ReportService]
            AiService[AiService]
        end

        %% Database Repositories
        subgraph Data Layer
            Repositories[Spring Data JPA Repositories]
        end
    end

    %% External Systems
    Database[(PostgreSQL DB)]
    LlmApi[LLM API / OpenAI or Gemini]

    %% Communications
    Browser <-->|HTTPS / REST / JWT| Web Layer
    
    %% Web to Service linkages
    AuthController --> AuthService
    ProjectController --> ProjectService
    TaskController --> TaskService
    TeamController --> TeamService
    PlanningController --> PlanningService
    ReportController --> ReportService

    %% Cross-Service dependencies
    PlanningService --> AiService
    PlanningService --> TaskService
    ReportService --> AiService
    ReportService --> ProjectService
    TaskService --> SchedulingService
    TaskService --> RiskService

    %% Service to DB
    ServiceModules -.->|Spring Data JPA| Repositories
    Repositories <-->|JDBC/Hibernate| Database
    AiService <-->|Spring AI RestClient| LlmApi
```

---

## Global Cross-Cutting Concerns

### 1. Security Configuration
* Configured via Spring Security with stateless sessions (`SessionCreationPolicy.STATELESS`).
* A custom `JwtAuthenticationFilter` interceptor parses the `Authorization: Bearer <token>` header, verifies the signature against the application key, and loads security context permissions.
* Method-level security annotations (e.g., `@PreAuthorize("hasRole('PM')")`) restrict administrative actions to project managers.

### 2. Global Exception Handling
A central controller advice annotated with `@RestControllerAdvice` catches runtime exceptions across all modules and returns structured HTTP error responses:

```json
{
  "timestamp": "2026-08-07T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Task dependency creates a cycle",
  "path": "/api/tasks/12/dependencies"
}
```

Standard exceptions caught:
* `MethodArgumentNotValidException`: Returns field-level validation errors.
* `EntityNotFoundException`: Returns a 404 status.
* `IllegalStateException` / `IllegalArgumentException`: Returns a 400 status.
* `AccessDeniedException`: Returns a 403 status.
