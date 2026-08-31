# Project Scope (MVP vs. Future Phases)

This document defines what is inside the Scope of the MVP (Minimum Viable Product) for the AI Project Manager platform and what is explicitly excluded or postponed.

---

## MVP Scope (In-Scope)

The MVP is designed as a standalone, web-based modular monolith. The core value is to enable AI-assisted planning combined with deterministic tracking and analytics.

1. **User Registration and Login**
   * Multi-role authentication (Project Manager and Team Member).
   * JWT-based secure sessions.

2. **Project Management & Dashboard**
   * Project configuration (name, description, deadline, budget, methodology, team size).
   * Dashboard compiling key statistics (overall progress, task breakdown, open risks, budget burn rate).

3. **Team Member Management**
   * Inviting or adding team members to the organization workspace.
   * Associating team members with active projects.

4. **Skills and Availability Management**
   * Custom skills database.
   * Mapping skills to team members with level ratings.
   * Track weekly available capacity (e.g. 40 hours/week) and project allocation percentage (0-100%).

5. **Task and Milestone Management**
   * Milestones (representing Waterfall project phases or Scrum Sprints).
   * Detailed tasks with priority, duration, assigned personnel, status, and description.

6. **Task Dependencies**
   * Standard Finish-to-Start (FS) dependencies between tasks within the same project.

7. **Gantt Chart Visualization**
   * Timeline view displaying milestones, tasks, start/end dates, progress, and dependency arrows.
   * Interactive UI to adjust task properties.

8. **Deterministic Project Progress Tracking**
   * Automatic calculation of project completion percentage (progress = sum of completed task weights or average progress of tasks).
   * Budget tracking based on task completion and user cost/effort allocation.

9. **Rule-Based Risk Detection**
   * Standard logic alerts (overdue tasks, critical path delays, budget run rates exceeding targets).

10. **Resource Workload & Bottleneck Detection**
    * Highlighting overloaded individuals whose total weekly task assignments exceed capacity.
    * Identifying bottleneck tasks (uncompleted tasks with many dependent downstream tasks).

11. **AI-Generated Project Plan**
    * Natural language plan generation via Spring Boot connection to LLM.
    * Previewing, editing, and approving plans before applying to DB.

12. **Weekly AI-Generated Status Report**
    * Automatic formatting of active data into a structured status report draft.
    * Review, edit, and publication capability for PMs.

---

## Explicitly Out-of-Scope (Postponed Features)

These features are deferred to future milestones to reduce complexity and focus on a robust core:

1. **External Project Management Integrations**
   * No integrations with **Jira**, **Asana**, **Monday.com**, or **Microsoft Project**. All planning and tracking occur natively inside the platform.

2. **Version Control Integrations**
   * No integrations with **GitHub**, **GitLab**, or **Bitbucket**. Progress must be updated manually by team members rather than auto-synced via pull requests/commits.

3. **Machine-Learning Risk Prediction**
   * No advanced probabilistic ML models. All risk detection must run on deterministic, rules-based business logic (e.g., date checks, workload capacity math).

4. **Automatic Employee Reassignment**
   * The AI will not automatically swap team members on tasks when overload is detected. It will only flag the risk and suggest options; the PM must perform adjustments manually.

5. **Enterprise-Level Multi-Tenant Permissions**
   * The MVP will support single-organization setups. Advanced multi-tenant isolation, custom sub-tenant billing, and granular directory integrations (e.g., SAML, Active Directory) are postponed.

6. **Microservices Architecture**
   * The application must be built as a clean **modular monolith** in Java 21 to simplify deployment, transactional integrity, and devops processes during the initial rollout.
