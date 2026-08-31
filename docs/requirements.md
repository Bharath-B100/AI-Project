# Functional and Non-Functional Requirements

This document outlines the detailed requirements for the AI Project Manager platform, specifying the roles, functional modules, validation rules, and system constraints.

---

## User Roles

The platform supports two main user roles:

1. **Project Manager (PM)**
   * Full administrative privileges over projects.
   * Can create, update, and delete projects, milestones, tasks, and team member assignments.
   * Can trigger AI project plan generation and weekly status reports.
   * Can approve or reject AI-generated plans and reports.
   
2. **Team Member**
   * Can view projects they are assigned to.
   * Can view their assigned tasks and the project Gantt chart.
   * Can update the status (e.g., *To Do*, *In Progress*, *Done*) and progress percentage (0-100%) of their assigned tasks.
   * Can view risk warnings and status reports.

---

## Functional Requirements (FR)

### 1. User Authentication & Profile (`auth`, `user` modules)
* **FR-1.1**: The system must allow users to register with email, password, full name, and role.
* **FR-1.2**: The system must authenticate users via email and password, returning a JWT token valid for 24 hours.
* **FR-1.3**: The system must enforce role-based access control (RBAC) on all secured REST endpoints.

### 2. Project Management (`project` module)
* **FR-2.1**: A PM can create a project by entering: Name, Description, Deadline, Budget, Team Size, and Methodology (Scrum/Waterfall).
* **FR-2.2**: The system must support viewing, listing, updating, and archiving/deleting projects.
* **FR-2.3**: The project dashboard must display project health metrics (progress, budget burn, risk count, task status distribution).

### 3. Team & Skill Management (`team` module)
* **FR-3.1**: A PM can manage a global registry of Skills (e.g., Java, React, SQL, QA).
* **FR-3.2**: A PM can register Team Members, define their weekly working hours capacity, and associate them with Skills alongside proficiency levels (1-5).
* **FR-3.3**: A PM can assign team members to specific projects with an allocation percentage (e.g., 50% allocation).

### 4. Task & Milestone Management (`task` module)
* **FR-4.1**: A PM can create Milestones (representing phases in Waterfall or Sprints in Scrum).
* **FR-4.2**: A PM can create Tasks within Milestones, specifying name, description, priority, estimated duration (in days), and status.
* **FR-4.3**: A PM can assign one or more Team Members to a task.

### 5. Task Dependencies (`task` module)
* **FR-5.1**: The system must support Finish-to-Start (FS) dependencies between tasks.
* **FR-5.2**: The system must prevent cyclic dependencies (e.g., Task A depends on Task B, which depends on Task A).
* **FR-5.3**: Dependency modifications must trigger a reschedule calculation.

### 6. Gantt Chart & Scheduling (`scheduling` module)
* **FR-6.1**: The system must provide a Gantt chart visualizing milestones, tasks, durations, and dependency lines.
* **FR-6.2**: The backend scheduling engine must calculate task start and end dates based on project start date, task durations, and dependencies (Critical Path Method calculation).

### 7. Rule-Based Risk & Workload Detection (`risk` module)
* **FR-7.1**: The system must calculate workload: if a team member’s total active task assignments in a week exceed their allocated capacity, flag a **Resource Overload** risk.
* **FR-7.2**: The system must detect schedule risks: if a task is not completed, its end date is in the past, and it lies on the critical path, flag a **Schedule Delay** risk.
* **FR-7.3**: The system must track budget risks: if actual logged effort/budget usage exceeds the proportional expected budget for the current project progress, flag a **Budget Overrun** risk.

### 8. AI Project Plan Generation (`planning`, `ai` modules)
* **FR-8.1**: A PM can prompt the system in natural language to generate a project plan.
* **FR-8.2**: The system must compile the prompt and send it to the LLM, retrieving a structured JSON project plan containing milestones, tasks, dependencies, durations, skills, assumptions, and risks.
* **FR-8.3**: The AI-generated plan must be stored in a staging table (`AiGeneration`) in `PENDING_APPROVAL` status. The PM must review and approve it before it is persisted to the live project/task tables.

### 9. Weekly Status Reports (`report`, `ai` modules)
* **FR-9.1**: The system must compile current project metrics (completed tasks, current risks, workload metrics) and use the LLM to draft a structured Markdown status report.
* **FR-9.2**: The report draft must be reviewable, editable, and approvable by the PM before being finalized.

---

## Non-Functional Requirements (NFR)

* **NFR-1 (Performance)**: Non-AI REST API calls must respond in under 300ms under normal load (up to 50 concurrent users). AI requests must complete within 15 seconds (using asynchronous endpoints or Server-Sent Events/Websockets if necessary, or clean REST timeouts).
* **NFR-2 (Security)**: All password storage must use BCrypt with a strength of 10+. JWT tokens must be signed using SHA-256 HMAC keys stored in environment variables. All requests must be sanitized for XSS and protected against SQL injection via JPA parameterized queries.
* **NFR-3 (Data Integrity)**: Database operations modifying multiple tables (e.g., approving an AI plan, deleting a milestone and cascading tasks) must be wrapped in JPA `@Transactional` blocks to ensure ACID compliance.
* **NFR-4 (Auditability)**: All AI generation actions must be logged with the original prompt, response, model name, tokens consumed, validation status, and the user ID of the PM who requested/approved it.
* **NFR-5 (Compatibility)**: The frontend application must be responsive, working on all modern desktop screens (1024px width and up) and tablets.

---

## Request Validation Rules

To prevent corruption and malicious inputs, the system enforces strict validation:

| Field | Type | Rules / Validation Annotations |
| :--- | :--- | :--- |
| **User Email** | String | `@Email`, `@NotBlank`, max 255 chars, unique in DB |
| **User Password** | String | `@Size(min = 8, max = 100)`, `@NotBlank`, must contain numbers and mixed case |
| **Project Name** | String | `@NotBlank`, `@Size(min = 3, max = 100)` |
| **Project Deadline** | Date | `@Future`, must be after the project start date |
| **Project Budget** | Decimal | `@DecimalMin(value = "0.0")`, `@NotNull` |
| **Task Duration** | Integer | `@Min(1)`, `@Max(365)`, duration in days |
| **Task Progress** | Integer | `@Min(0)`, `@Max(100)`, represents percentage |
| **Allocation %** | Integer | `@Min(1)`, `@Max(100)`, percentage allocation |
