# REST API Design Specification

This document details the REST API endpoints, HTTP methods, request/response JSON schemas, and authorization levels for the AI Project Manager platform.

---

## Endpoint Summary Table

### Authentication
* `POST /api/auth/register` (Public) - Create user account.
* `POST /api/auth/login` (Public) - Authenticate credentials and get JWT token.

### Project Management
* `GET /api/projects` (Authenticated) - List all projects.
* `POST /api/projects` (PM Only) - Create project shell.
* `GET /api/projects/{id}` (Authenticated) - Get detailed project shell metadata.
* `PUT /api/projects/{id}` (PM Only) - Update project properties.
* `DELETE /api/projects/{id}` (PM Only) - Archive or delete project.
* `GET /api/projects/{id}/dashboard` (Authenticated) - Get aggregated metrics (progress, budget burn, risk count).

### Team and Skills
* `GET /api/skills` (Authenticated) - List global skill list.
* `POST /api/skills` (PM Only) - Create a new skill in registry.
* `GET /api/team-members` (Authenticated) - List all system team members.
* `POST /api/team-members` (PM Only) - Create team member profile.
* `GET /api/projects/{id}/team` (Authenticated) - Get team members assigned to project.
* `POST /api/projects/{id}/team` (PM Only) - Assign team member to project with allocation percentage.

### Tasks, Milestones, and Dependencies
* `GET /api/projects/{id}/tasks` (Authenticated) - List project tasks and milestones.
* `POST /api/projects/{id}/tasks` (PM Only) - Create new task manually.
* `PUT /api/tasks/{id}` (PM Only) - Update task name, description, duration, and priority.
* `PATCH /api/tasks/{id}/status` (Assigned Member or PM) - Update status and progress.
* `DELETE /api/tasks/{id}` (PM Only) - Delete a task.
* `POST /api/tasks/{id}/dependencies` (PM Only) - Add a task dependency link.
* `DELETE /api/tasks/{id}/dependencies/{predecessorId}` (PM Only) - Remove a dependency link.

### Scheduling (Gantt Chart)
* `GET /api/projects/{id}/schedule` (Authenticated) - Fetch task start/end dates for Gantt rendering.
* `POST /api/projects/{id}/schedule/calculate` (PM Only) - Recalculate scheduling timelines based on durations and dependencies.

### AI Planning Integration
* `POST /api/projects/{id}/planning/generate` (PM Only) - Trigger AI project plan generation.
* `GET /api/planning/{generationId}` (PM Only) - Fetch staged project plan.
* `POST /api/planning/{generationId}/approve` (PM Only) - Approve plan, write to DB tasks, and trigger schedule calculation.
* `POST /api/planning/{generationId}/reject` (PM Only) - Discard staged plan.

### Risk Management
* `GET /api/projects/{id}/risks` (Authenticated) - Fetch list of active/resolved risks.
* `PATCH /api/risks/{id}/resolve` (PM Only) - Manually resolve a detected risk.

### Weekly Reporting
* `POST /api/projects/{id}/reports/draft` (PM Only) - Trigger AI status report draft compilation.
* `GET /api/reports/{id}` (Authenticated) - Fetch status report details.
* `PUT /api/reports/{id}` (PM Only) - Edit report draft before publishing.
* `POST /api/reports/{id}/publish` (PM Only) - Finalize and archive report.

---

## Detailed Payload Schemas

### 1. User Registration (`POST /api/auth/register`)
**Request:**
```json
{
  "email": "manager@company.com",
  "password": "SecurePassword123!",
  "fullName": "Jane Doe",
  "role": "PM"
}
```
**Response (201 Created):**
```json
{
  "id": 101,
  "email": "manager@company.com",
  "fullName": "Jane Doe",
  "role": "ROLE_PM"
}
```

### 2. Project Creation (`POST /api/projects`)
**Request:**
```json
{
  "name": "E-Commerce Frontend Rebuild",
  "description": "Redesigning the shopping cart UI to improve conversion.",
  "startDate": "2026-09-01",
  "deadline": "2026-12-15",
  "budget": 50000.00,
  "methodology": "SCRUM",
  "teamSize": 5
}
```
**Response (201 Created):**
```json
{
  "id": 1,
  "name": "E-Commerce Frontend Rebuild",
  "status": "PLANNING",
  "budget": 50000.00,
  "startDate": "2026-09-01",
  "deadline": "2026-12-15",
  "methodology": "SCRUM",
  "teamSize": 5,
  "createdAt": "2026-08-07T12:00:00Z"
}
```

### 3. Fetch Gantt Schedule (`GET /api/projects/1/schedule`)
**Response (200 OK):**
```json
{
  "projectId": 1,
  "projectName": "E-Commerce Frontend Rebuild",
  "startDate": "2026-09-01",
  "endDate": "2026-10-14",
  "milestones": [
    {
      "milestoneName": "Sprint 1 - Foundation",
      "tasks": [
        {
          "id": 12,
          "name": "Setup Project Boilerplate",
          "startDate": "2026-09-01",
          "endDate": "2026-09-05",
          "durationDays": 4,
          "status": "TODO",
          "progressPercentage": 0,
          "dependencies": []
        },
        {
          "id": 13,
          "name": "Configure Database Schema",
          "startDate": "2026-09-05",
          "endDate": "2026-09-09",
          "durationDays": 4,
          "status": "TODO",
          "progressPercentage": 0,
          "dependencies": [12]
        }
      ]
    }
  ]
}
```

### 4. Trigger AI Planning (`POST /api/projects/1/planning/generate`)
**Request:**
```json
{
  "prompt": "Create a plan for developing a secure login flow using OAuth2 and Spring Security, with complete integration tests."
}
```
**Response (202 Accepted):**
```json
{
  "generationId": 45,
  "projectId": 1,
  "status": "PENDING_APPROVAL",
  "message": "AI project plan generated. Staged and waiting for review."
}
```
