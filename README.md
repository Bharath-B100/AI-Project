# AI Project Manager

AI Project Manager is an AI-powered project management platform. Project managers can describe their goals in natural language to construct milestones, tasks, and dependencies, which are reviewed/edited in a staging view, and then managed and monitored through deterministic backend analytics and risk logs.

---

## Project Structure

```text
ai-project-manager/
├── docs/                     # Architectural specifications and designs
├── backend/                  # Spring Boot Java Backend
│   ├── pom.xml               # Maven configuration
│   └── src/
│       ├── main/
│       │   ├── java/com/aipm/
│       │   │   ├── App.java  # Main App Class
│       │   │   └── auth/     # Security structures (SecurityConfig)
│       │   └── resources/
│       │       └── application.yml  # Database configs
│       └── test/             # JUnit integration and unit tests
├── frontend/                 # React TypeScript Frontend
│   ├── package.json          # Node configuration
│   ├── tsconfig.json         # TypeScript configuration
│   ├── vite.config.ts        # Vite configuration & proxy settings
│   ├── index.html            # Web template
│   └── src/
│       ├── App.tsx           # Premium React Hero screen
│       ├── main.tsx          # React render bridge
│       ├── index.css         # Visual glassmorphism styling
│       └── test/             # Vitest setup and unit tests
├── docker-compose.yml        # PostgreSQL container setup
├── .env.example              # Environments template
└── README.md                 # Running instructions
```

---

## Getting Started

### 1. Run PostgreSQL database
To spin up a local PostgreSQL instance via Docker:
```bash
docker compose up -d
```
The database defaults to port `5432` with username `aipm_user` and database name `aipm_db` as configured in the `docker-compose.yml` file.

### 2. Run Backend Application
Ensure you have Java 21 and Maven installed. Navigating to the `backend/` directory, execute:
```bash
cd backend
mvn spring-boot:run
```
The server will run on `http://localhost:8080`.
The OpenAPI documentation (Swagger UI) is available at `http://localhost:8080/swagger-ui.html`.

### 3. Run Frontend Application
Ensure you have Node.js (v18+) installed. Navigating to the `frontend/` directory, install packages and execute the Vite development server:
```bash
cd frontend
npm install
npm run dev
```
The frontend website runs locally on `http://localhost:3000` (which is configured to proxy all `/api` requests to the backend server automatically).

---

## Test Executions

### Backend Tests
To run Java unit and context validation tests:
```bash
cd backend
mvn clean test
```

### Frontend Tests
To run Vitest verification suites:
```bash
cd frontend
npm run test
```
