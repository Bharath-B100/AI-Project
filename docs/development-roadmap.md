# Development Roadmap and Milestones

This document details the phased rollout plan for the AI Project Manager platform, establishing clear milestones, expected deliverables, and task lists.

---

## Phase 1: Scaffold & Infrastructure Setup (Milestone 1)
*Goal: Establish empty project wrappers, configuration layers, data pipelines, and verified container builds.*

* **Tasks**:
  * Set up Docker Compose for local PostgreSQL database.
  * Initialize the Spring Boot 21 Maven application with required modules: `web`, `data-jpa`, `security`, `validation`, `flyway/liquibase`, and `test`.
  * Initialize the React + TypeScript frontend application with Vite.
  * Set up standard configuration files: `.gitignore`, `README.md`, `.env.example`, and Maven `pom.xml`.
  * Implement CORS filters, global exception middleware skeletons, and basic database connection sanity test suites.
* **Verification**: Verify project compile and green status for both backend and frontend initial test targets (`mvn clean test` and `npm run test` exits 0).

---

## Phase 2: Core Task & Project Management (Milestone 2)
*Goal: Deliver full CRUD capability for Projects and Tasks, complete dependency validation, and the Gantt visualization screen.*

* **Tasks**:
  * **Auth & User**: Implement JWT login, user registration, role authorization middleware.
  * **Project Module**: Core CRUD APIs for `Project`.
  * **Task & Milestone Module**: Core CRUD APIs for `Task` and Milestones.
  * **Dependency Validation**: Write backend constraint checks. Verify that any incoming task dependency does not generate cyclic loops.
  * **Scheduling Engine**: Implement a Critical Path Method (CPM) scheduler in Java. Automatically update tasks' start/end dates based on duration and dependency rules.
  * **Frontend Gantt**: Integrate an interactive Gantt chart viewer showing milestones, tasks, start/end dates, and dependency arrows.
* **Verification**: Unit and Integration tests on Project and Task controllers. CPM scheduling validations (e.g. chaining 3 tasks with 5 days each shifts dates correctly).

---

## Phase 3: Analytics, Workload, and Risk Engines (Milestone 3)
*Goal: Implement deterministic progress calculators, workload capacity tracking, and rule-based risk warning alerts.*

* **Tasks**:
  * **Progress Tracker**: Implement calculations for weighted task progress at project and milestone levels.
  * **Capacity & Team Assignments**: Implement API to register skills, map team members, and assign members to projects.
  * **Workload Analyzer**: Write backend calculations checking weekly total allocations across tasks. If sum > capacity, write a `Risk` entry.
  * **Date & Budget Audit**: Implement rule-based detection for task delays (uncompleted tasks with end dates in the past) and budget overruns.
  * **Dashboard UI**: Design a responsive overview panel in React containing metrics widgets (progress bars, risk count cards, budget dials).
* **Verification**: Mock datasets validation. Confirm that assigning 45 hours of work to a 40-hour capacity team member automatically registers a "Resource Overload" risk record in the database.

---

## Phase 4: AI Integration & Staging Pipelines (Milestone 4)
*Goal: Connect the LLM client, implement structured JSON parsing, build approval staging screens, and compile status reports.*

* **Tasks**:
  * **Spring AI Integration**: Configure client wrappers, set model configuration properties, and implement API call safety triggers (timeouts).
  * **AI Planning Staging**: Create the `AiGeneration` table. Build planning prompt wrappers. Parse structural JSON responses. Show preview/diff pages on frontend.
  * **Report Module**: Add API to run metrics calculations, construct system prompts, trigger Markdown status report drafts, and save approved drafts.
  * **End-to-End Polish**: Final integration testing, styling improvements, and error-state user flows testing.
* **Verification**: Run structured JSON schema validations against mock LLM outputs. Verify that a PM can successfully prompt the AI, view the draft tasks, modify one task, and click "Approve" to write them into active database lists.
