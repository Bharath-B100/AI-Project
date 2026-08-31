# 🤖 AI Project Manager (Enterprise Edition)

[![Repository](https://img.shields.io/badge/GitHub-Repository-blue?logo=github)](https://github.com/Bharath-B100/AI-Project)
[![Java](https://img.shields.io/badge/Java-17%20LTS-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue?logo=react)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.x-blue?logo=typescript)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)](https://www.postgresql.org/)

An enterprise-grade project management platform where **Artificial Intelligence** automates planning, risk assessment, schedule generation, resource leveling, budget tracking, and executive status reporting.

---

## 🌟 Key Features

| Feature | Description |
|:---|:---|
| 🧠 **AI Natural Language Planner** | Converts plain English project scopes into structured tasks, milestones, durations, complexity scores, and dependency networks. |
| 📊 **Auto-Gantt & CPM Engine** | Kahn's algorithm topological sorting with Critical Path Method (CPM) calculation, slack analysis, and business calendar skipping weekends & regional holidays. |
| ⚠️ **Probabilistic Risk Engine** | Monte Carlo simulation with P10/P50/P90 delay probability forecasts and proactive risk mitigation advice. |
| 🔄 **What-If Scenario Simulator** | Models the impact of adding developers, reducing scope, or extending timelines before committing changes. |
| 👥 **Skill-Based Resource Leveling** | Detects bottlenecks, flags overloaded members (>100% capacity), and computes 1-click workload redistribution. |
| 🏃 **Agile & Waterfall Support** | Full Agile Sprint management with burndown velocity points alongside traditional Waterfall Gantt views. |
| 📈 **Progress & Budget Tracking** | Live progress variance (`actualProgress` vs `expectedProgress`), earned value analysis, and budget health categorization. |
| 📑 **Executive Reports & PDF Export** | Auto-generates weekly RAG status reports with KPI summaries, AI narratives, milestone snapshots, and multi-page PDF downloads. |
| 🔌 **External Tool Integrations** | Pre-built connectors for **Jira**, **GitHub**, **GitLab**, **Asana**, **Monday.com**, and **MS Project**. |

---

## 🏗️ Architecture & Tech Stack

```
Frontend (React 18 + Vite 5 + TypeScript)
   │ (Axios API Client + JWT Bearer Interceptor)
   ▼
Backend (Spring Boot 3.2.5 + Java 17 LTS)
   ├── Security: JJWT (Stateless HMAC-SHA256)
   ├── ORM: Spring Data JPA + Hibernate 6
   ├── Database Migration: Flyway (V1–V4)
   ├── API Documentation: SpringDoc OpenAPI (Swagger UI)
   └── Core Modules:
       ├── /ai & /planning      → NLP task generation & complexity scoring
       ├── /scheduling          → CPM engine & Business calendar
       ├── /risk                → Monte Carlo & Rule-based risk analytics
       ├── /simulation          → What-If scenario engine
       ├── /tracking            → Progress variance, budget burn, workload
       ├── /report              → StatusReportDTO & Zero-dependency PDF export
       └── /integration         → Multi-provider external synchronization
   │
   ▼
Database: PostgreSQL 16 (or H2 for unit testing)
```

---

## 🚀 Getting Started

### Prerequisites
- **Java JDK 17+**
- **Node.js 18+** & **npm**
- **Docker & Docker Compose** (for PostgreSQL)

---

### 1. Clone the Repository
```bash
git clone https://github.com/Bharath-B100/AI-Project.git
cd AI-Project
```

### 2. Configure Environment (Optional)
```bash
cp .env.example .env
```

### 3. Start Database (PostgreSQL)
```bash
docker compose up -d
```
*PostgreSQL will be running on `localhost:5432` with database `aipm_db`.*

### 4. Start Backend Server
```bash
cd backend
mvn spring-boot:run
```
*Backend runs on `http://localhost:8080`.*  
*Interactive Swagger API documentation: **`http://localhost:8080/swagger-ui.html`***

### 5. Start Frontend Client
```bash
cd frontend
npm install
npm run dev
```
*Frontend runs on `http://localhost:5173` (configured to proxy API calls to `:8080`).*

---

## 🔑 Demo Login Credentials

The application automatically seeds a demo user on first startup:
- **Email:** `demo@example.com`
- **Password:** `password`

---

## 🧪 Testing & Verification

### Run Backend Unit & Integration Tests
```bash
cd backend
mvn clean test
```

### Run Frontend TypeScript Validation & Build
```bash
cd frontend
npm run build
```

---

## 📂 Project Directory Structure

```text
AI-Project/
├── docs/                     # Architectural diagrams and specifications
├── backend/                  # Spring Boot 3.2.5 Backend
│   ├── pom.xml               # Maven configuration
│   ├── src/main/java/com/example/aiprojectmanager/
│   │   ├── ai/               # AI prompts & services
│   │   ├── assignment/       # Resource allocation logic
│   │   ├── auth/             # JWT security filter & user details
│   │   ├── common/           # Error handlers & exceptions
│   │   ├── config/           # AppConfig, DemoDataSeeder, SecurityConfig
│   │   ├── dashboard/        # Portfolio KPI aggregation
│   │   ├── integration/      # Jira/GitHub/Asana/Monday/MSP sync
│   │   ├── planning/         # Natural language planner
│   │   ├── project/          # Project & Sprint CRUD
│   │   ├── report/           # Weekly executive report & PDF generator
│   │   ├── risk/             # Predictive risk & Monte Carlo
│   │   ├── scheduling/       # CPM schedule generator & business calendar
│   │   ├── simulation/       # What-if scenario simulator
│   │   ├── task/             # Task & dependency management
│   │   ├── team/             # Team members & skill matrices
│   │   ├── tracking/         # Progress & budget tracking
│   │   └── user/             # User domain & repository
│   └── src/main/resources/
│       ├── application.yml   # Multi-profile configurations
│       └── db/migration/     # Flyway SQL migrations (V1–V4)
├── frontend/                 # React 18 TypeScript Frontend
│   ├── package.json          # Node dependencies
│   ├── vite.config.ts        # Vite configuration & /api proxy
│   └── src/
│       ├── components/       # Gantt, Planner Modal, What-If Modal, etc.
│       ├── context/          # Auth context state
│       ├── layouts/          # Responsive AppLayout with sidebar
│       ├── pages/            # 11 full feature pages
│       ├── services/         # api.ts (Axios REST client with typed DTOs)
│       └── index.css         # Glassmorphism design system
├── docker-compose.yml        # PostgreSQL container setup
├── .env.example              # Environment variables template
├── remaining.md              # Requirement & implementation checklist
└── README.md                 # Complete documentation
```

---

## 📜 License

This project is licensed under the MIT License.
