import axios from 'axios';

export type ProjectStatus = 'DRAFT' | 'ACTIVE' | 'COMPLETED' | 'ON_HOLD';
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'BLOCKED' | 'DONE';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type DependencyType = 'FINISH_TO_START' | 'START_TO_START' | 'FINISH_TO_FINISH' | 'START_TO_FINISH';

export interface Project {
  id: number;
  ownerId?: number;
  name: string;
  description?: string;
  startDate?: string;
  endDate?: string;
  budget?: number;
  methodology?: string;
  status: ProjectStatus;
}

export interface Task {
  id: number;
  projectId?: number;
  title: string;
  description?: string;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate?: string;
  startDate?: string;
  estimatedHours?: number;
  actualHours?: number;
  progressPercentage: number;
  durationDays?: number;
}

export interface Dependency {
  id: number;
  projectId: number;
  predecessorTaskId: number;
  successorTaskId: number;
  dependencyType: DependencyType;
  lagDays: number;
}

export interface GanttTask {
  id: number;
  name: string;
  scheduledStart: string;
  scheduledEnd: string;
  durationDays: number;
  progressPercentage: number;
  dependencies: number[];
  isCritical: boolean;
  status: TaskStatus;
  priority: TaskPriority;
}

export interface GanttData {
  tasks: GanttTask[];
  projectStart: string;
  projectEnd: string;
  criticalPath: number[];
}

export interface CriticalPathTask {
  id: number;
  name: string;
  durationDays: number;
  scheduledStart: string;
  scheduledEnd: string;
}

export interface CriticalPath {
  tasks: CriticalPathTask[];
  totalDurationDays: number;
}

export interface ScheduleCalculation {
  calculatedTasks: GanttTask[];
  criticalPath: number[];
  totalDurationDays: number;
}

export type ProjectInput = Omit<Project, 'id' | 'ownerId'>;
export type TaskInput = Omit<Task, 'id' | 'projectId'>;

const getStoredToken = (): string | null => {
  try {
    return localStorage.getItem('jwtToken') || sessionStorage.getItem('jwtToken');
  } catch {
    return null;
  }
};

let authToken: string | null = getStoredToken();
let onUnauthorized: (() => void) | null = null;

export const setAuthToken = (token: string | null) => {
  authToken = token;
};

export const registerUnauthorizedCallback = (callback: () => void) => {
  onUnauthorized = callback;
};

const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const token = authToken || getStoredToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
}, (error) => {
  return Promise.reject(error);
});

api.interceptors.response.use((response) => {
  return response;
}, (error) => {
  if (error.response && error.response.status === 401) {
    if (onUnauthorized) {
      onUnauthorized();
    }
  }
  return Promise.reject(error);
});

export const projectApi = {
  list: () => api.get<Project[]>('/projects').then((r) => r.data),
  get: (id: number) => api.get<Project>(`/projects/${id}`).then((r) => r.data),
  create: (v: ProjectInput) => api.post<Project>('/projects', v).then((r) => r.data),
  update: (id: number, v: ProjectInput) => api.put<Project>(`/projects/${id}`, v).then((r) => r.data),
  remove: (id: number) => api.delete(`/projects/${id}`),
};

export const taskApi = {
  list: (projectId: number) => api.get<Task[]>(`/projects/${projectId}/tasks`).then((r) => r.data),
  create: (projectId: number, v: TaskInput) => api.post<Task>(`/projects/${projectId}/tasks`, v).then((r) => r.data),
  get: (id: number) => api.get<Task>(`/tasks/${id}`).then((r) => r.data),
  update: (id: number, v: TaskInput) => api.put<Task>(`/tasks/${id}`, v).then((r) => r.data),
  remove: (id: number) => api.delete(`/tasks/${id}`),
  status: (id: number, status: TaskStatus) => api.patch<Task>(`/tasks/${id}/status`, { status }).then((r) => r.data),
  priority: (id: number, priority: TaskPriority) => api.patch<Task>(`/tasks/${id}/priority`, { priority }).then((r) => r.data),
  progress: (id: number, progressPercentage: number) => api.patch<Task>(`/tasks/${id}/progress`, { progressPercentage }).then((r) => r.data),
};

export const dependencyApi = {
  list: (projectId: number) =>
    api.get<Dependency[]>(`/projects/${projectId}/dependencies`).then((r) => r.data),
  create: (projectId: number, taskId: number, body: {
    predecessorTaskId: number;
    dependencyType?: DependencyType;
    lagDays?: number;
  }) =>
    api.post<Dependency>(`/projects/${projectId}/tasks/${taskId}/dependencies`, body).then((r) => r.data),
  remove: (projectId: number, dependencyId: number) =>
    api.delete(`/projects/${projectId}/dependencies/${dependencyId}`),
};

export interface AutoLevelResponse {
  projectId: number;
  totalTasks: number;
  leveledTasks: number;
  resolvedResourceConflicts: number;
  originalProjectEnd: string;
  leveledProjectEnd: string;
  delayOrSavedDays: number;
  tasks: GanttTask[];
  levelingLog: string[];
}

export const scheduleApi = {
  calculate: (projectId: number) =>
    api.post<ScheduleCalculation>(`/projects/${projectId}/schedule/calculate`).then((r) => r.data),
  autoLevel: (projectId: number) =>
    api.post<AutoLevelResponse>(`/projects/${projectId}/schedule/auto-level`).then((r) => r.data),
  getGantt: (projectId: number) =>
    api.get<GanttData>(`/projects/${projectId}/gantt`).then((r) => r.data),
  getCriticalPath: (projectId: number) =>
    api.get<CriticalPath>(`/projects/${projectId}/critical-path`).then((r) => r.data),
};

// Milestone 4: Team, Assignment, Tracking, Risks

export interface TeamMember {
  id: number;
  projectId: number;
  name: string;
  email: string;
  role?: string;
  timezone?: string;
  hourlyRate?: number;
  availabilityHoursPerWeek: number;
  active: boolean;
}

export interface TaskAssignment {
  id: number;
  taskId: number;
  teamMemberId: number;
  teamMemberName: string;
  allocationPercentage: number;
  plannedHours: number;
  actualHours?: number;
  assignedAt: string;
}

export interface AssignTaskRequest {
  teamMemberId: number;
  allocationPercentage: number;
  plannedHours: number;
}

export interface ProjectHealth {
  actualProgress: number;
  expectedProgress: number;
  progressVariance: number;
  totalTasks: number;
  completedTasks: number;
  overdueTasks: number;
  projectHealth: 'ON_TRACK' | 'AT_RISK' | 'OFF_TRACK';
}

export interface TeamWorkload {
  teamMemberId: number;
  teamMemberName: string;
  assignedTaskCount: number;
  plannedHours: number;
  actualHours: number;
  availableHours: number;
  utilizationPercentage: number;
  workloadStatus: 'AVAILABLE' | 'NEAR_CAPACITY' | 'OVERLOADED';
}

export interface ProjectWorkload {
  projectId: number;
  teamWorkloads: TeamWorkload[];
}

export interface BudgetHealth {
  approvedBudget: number;
  actualCost: number;
  remainingBudget: number;
  budgetUsedPercentage: number;
  estimatedLaborCost: number;
  budgetHealth: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
}

export type RiskType = 'SCHEDULE_VARIANCE' | 'RESOURCE_OVERLOAD' | 'BUDGET_OVERRUN' | 'SCOPE_CREEP' | 'BLOCKED_TASK' | 'OVERDUE_TASK';
export type RiskSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type RiskStatus = 'OPEN' | 'MITIGATED' | 'ACCEPTED' | 'CLOSED';

// Matches backend ProjectRiskDTO exactly
export interface ProjectRisk {
  id: number;
  projectId: number;
  riskType: string;          // RiskType enum value as string
  severity: string;          // RiskSeverity enum value as string
  riskScore: number;         // backend field name is riskScore
  title: string;
  description: string;
  evidenceJson?: string;
  suggestedAction?: string;  // backend field name is suggestedAction
  status: string;            // RiskStatus enum value as string
  detectedAt: string;
}

// Cost entries (budget tracking)
export type CostCategory = 'LABOR' | 'MATERIAL' | 'EQUIPMENT' | 'OVERHEAD' | 'OTHER';
export interface CostEntry {
  id: number;
  category: CostCategory;
  description?: string;
  amount: number;
  entryDate: string;
}
export interface CreateCostEntryRequest {
  category: CostCategory;
  description?: string;
  amount: number;
  entryDate: string;   // ISO date string YYYY-MM-DD
  taskId?: number;
}

export const teamApi = {
  list: (projectId: number) => api.get<TeamMember[]>(`/projects/${projectId}/team-members`).then(r => r.data),
  add: (projectId: number, data: Partial<TeamMember>) => api.post<TeamMember>(`/projects/${projectId}/team-members`, data).then(r => r.data),
  update: (projectId: number, memberId: number, data: Partial<TeamMember>) => api.put<TeamMember>(`/projects/${projectId}/team-members/${memberId}`, data).then(r => r.data),
  remove: (projectId: number, memberId: number) => api.delete(`/projects/${projectId}/team-members/${memberId}`),
};

export const assignmentApi = {
  list: (_projectId: number, taskId: number) => api.get<TaskAssignment[]>(`/tasks/${taskId}/assignments`).then(r => r.data),
  assign: (_projectId: number, taskId: number, data: AssignTaskRequest) => api.post<TaskAssignment>(`/tasks/${taskId}/assignments`, data).then(r => r.data),
  remove: (_projectId: number, taskId: number, assignmentId: number) => api.delete(`/tasks/${taskId}/assignments/${assignmentId}`),
};

export const trackingApi = {
  getProgress: (projectId: number) => api.get<ProjectHealth>(`/projects/${projectId}/progress`).then(r => r.data),
  getWorkload:  (projectId: number) => api.get<ProjectWorkload>(`/projects/${projectId}/workload`).then(r => r.data),
  getBudget:    (projectId: number) => api.get<BudgetHealth>(`/projects/${projectId}/budget-health`).then(r => r.data),
};

export const riskApi = {
  list:          (projectId: number) => api.get<ProjectRisk[]>(`/projects/${projectId}/risks`).then(r => r.data),
  analyze:       (projectId: number) => api.post<void>(`/projects/${projectId}/risks/analyze`).then(() => undefined),  // returns void
  updateStatus:  (projectId: number, riskId: number, status: string) => api.patch<ProjectRisk>(`/projects/${projectId}/risks/${riskId}/status`, { status }).then(r => r.data),
};

export const costApi = {
  list:   (projectId: number) => api.get<CostEntry[]>(`/projects/${projectId}/costs`).then(r => r.data),
  add:    (projectId: number, data: CreateCostEntryRequest) => api.post<CostEntry>(`/projects/${projectId}/costs`, data).then(r => r.data),
};

export interface DashboardOverview {
  totalProjects: number;
  activeProjects: number;
  totalBudget: number;
  totalSpent: number;
  globalHealthStatus: 'ON_TRACK' | 'AT_RISK' | 'OFF_TRACK' | 'IDLE';
  globalWorkloadStatus: 'AVAILABLE' | 'NEAR_CAPACITY' | 'OVERLOADED' | 'IDLE';
  recentProjects: Project[];
}

export const dashboardApi = {
  getOverview: () => api.get<DashboardOverview>('/dashboard/overview').then(r => r.data),
};

export interface UserProfile { id: number; name: string; email: string; }

export const profileApi = {
  getMe:          () => api.get<UserProfile>('/auth/me').then(r => r.data),
  updateName:     (name: string) => api.patch<UserProfile>('/auth/me', { name }).then(r => r.data),
  changePassword: (currentPassword: string, newPassword: string) =>
    api.post<{ message: string }>('/auth/change-password', { currentPassword, newPassword }).then(r => r.data),
};

// ── AI Planning Suite ────────────────────────────────────────────────────────
export interface PlanGenerationRequest {
  prompt: string;
  timelineMonths?: number;
  teamSize?: number;
  budget?: number;
  methodology?: string;
}

export interface GeneratedMilestone {
  name: string;
  description: string;
  targetDayOffset: number;
}

export interface GeneratedTask {
  tempId: string;
  title: string;
  description: string;
  estimatedHours: number;
  durationDays: number;
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  milestone: string;
  requiredSkills: string[];
  dependsOnTempIds: string[];
}

export interface GeneratedPlan {
  projectName: string;
  description: string;
  suggestedMethodology: string;
  estimatedTotalDays: number;
  recommendedBudget: number;
  recommendedRoles: string[];
  milestones: GeneratedMilestone[];
  tasks: GeneratedTask[];
}

export interface CommitPlanRequest {
  projectName: string;
  description: string;
  methodology: string;
  budget: number;
  startDate?: string;
  tasks: GeneratedTask[];
}

export interface CommitPlanResult {
  projectId: number;
  projectName: string;
  taskCount: number;
  dependencyCount: number;
  schedule?: any;
}

export interface SuggestedDependency {
  predecessorTaskId: number;
  predecessorTitle: string;
  successorTaskId: number;
  successorTitle: string;
  dependencyType: string;
  rationale: string;
  confidenceScore: number;
}

export interface PlanRefinementRequest {
  instruction: string;
  currentPlan: GeneratedPlan;
}

export const planningApi = {
  generatePlan:        (req: PlanGenerationRequest) => api.post<GeneratedPlan>('/planning/generate', req).then(r => r.data),
  refinePlan:          (req: PlanRefinementRequest) => api.post<GeneratedPlan>('/planning/refine', req).then(r => r.data),
  suggestDependencies: (projectId: number)          => api.get<SuggestedDependency[]>(`/planning/suggest-dependencies/${projectId}`).then(r => r.data),
  commitPlan:          (req: CommitPlanRequest)   => api.post<CommitPlanResult>('/planning/commit', req).then(r => r.data),
};

// ── What-If Scenario Simulation Suite ─────────────────────────────────────────
export interface TaskSimulationOverride {
  taskId: number;
  durationDeltaDays?: number;
  newDurationDays?: number;
  excludeFromScope?: boolean;
}

export interface SimulationRequest {
  developerDelta?: number;
  developerHourlyRate?: number;
  productivityMultiplier?: number;
  taskOverrides?: TaskSimulationOverride[];
}

export interface SimulationResult {
  projectId: number;
  baselineDurationDays: number;
  baselineFinishDate: string;
  baselineEstimatedCost: number;
  baselineCriticalPathLength: number;
  simulatedDurationDays: number;
  simulatedFinishDate: string;
  simulatedEstimatedCost: number;
  simulatedCriticalPathLength: number;
  durationDeltaDays: number;
  costDelta: number;
  feasibilityAssessment: 'OPTIMAL' | 'FEASIBLE' | 'HIGH_RISK' | 'DIMINISHING_RETURNS';
  prescriptiveRecommendations: string[];
  simulatedTasks: GanttTask[];
  simulatedCriticalPath: number[];
}

export const simulationApi = {
  simulate: (projectId: number, req?: SimulationRequest) =>
    api.post<SimulationResult>(`/projects/${projectId}/simulate`, req || {}).then(r => r.data),
  apply: (projectId: number, req?: SimulationRequest) =>
    api.post<{ applied: boolean; updatedTasksCount: number; newDurationDays: number; newFinishDate: string; message: string }>(
      `/projects/${projectId}/simulate/apply`, req || {}
    ).then(r => r.data),
};

// ── Intelligent Resource Leveling Suite ───────────────────────────────────────
export interface LevelingRecommendation {
  taskId: number;
  taskTitle: string;
  taskDurationDays: number;
  plannedHours: number;
  sourceMemberId: number;
  sourceMemberName: string;
  sourceCurrentWorkloadPct: number;
  targetMemberId: number;
  targetMemberName: string;
  targetCurrentWorkloadPct: number;
  rationale: string;
}

export interface LevelingReport {
  projectId: number;
  totalTeamMembers: number;
  overloadedCount: number;
  availableCount: number;
  portfolioWorkloadStatus: 'OPTIMAL' | 'OVERLOADED' | 'UNBALANCED';
  recommendations: LevelingRecommendation[];
}

export const levelingApi = {
  getRecommendations: (projectId: number) =>
    api.get<LevelingReport>(`/projects/${projectId}/leveling/recommendations`).then(r => r.data),
  apply: (projectId: number) =>
    api.post<{ appliedReallocations: number; message: string }>(`/projects/${projectId}/leveling/apply`).then(r => r.data),
};

// ── Probabilistic Risk & Delay Prediction Suite ───────────────────────────────
export interface PredictiveRiskReport {
  projectId: number;
  projectName: string;
  delayProbabilityPercentage: number;
  riskLevel: 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL';
  predictedDelayDays: number;
  p10FinishDate: string;
  p50FinishDate: string;
  p90FinishDate: string;
  topRiskDrivers: string[];
  similarHistoricalProjectsCount: number;
  similarityAssessment: string;
  recommendedRemediation: string;
}

export const predictiveRiskApi = {
  getPredictions: (projectId: number) =>
    api.get<PredictiveRiskReport>(`/projects/${projectId}/risks/predictions`).then(r => r.data),
};

// ── Sprint Management ─────────────────────────────────────────────────────────
export interface Sprint {
  id: number;
  projectId: number;
  name: string;
  goal?: string;
  startDate?: string;
  endDate?: string;
  status: 'PLANNED' | 'ACTIVE' | 'COMPLETED';
  velocityPoints: number;
  totalTasks: number;
  completedTasks: number;
  totalStoryPoints: number;
  completedStoryPoints: number;
  completionPct: number;
}

export type SprintInput = Omit<Sprint, 'id' | 'projectId' | 'totalTasks' | 'completedTasks' | 'totalStoryPoints' | 'completedStoryPoints' | 'completionPct'>;

export const sprintApi = {
  list:         (projectId: number) => api.get<Sprint[]>(`/projects/${projectId}/sprints`).then(r => r.data),
  create:       (projectId: number, data: Partial<SprintInput>) => api.post<Sprint>(`/projects/${projectId}/sprints`, data).then(r => r.data),
  start:        (projectId: number, sprintId: number) => api.post<Sprint>(`/projects/${projectId}/sprints/${sprintId}/start`).then(r => r.data),
  complete:     (projectId: number, sprintId: number) => api.post<Sprint>(`/projects/${projectId}/sprints/${sprintId}/complete`).then(r => r.data),
  assignTask:   (projectId: number, sprintId: number, taskId: number) => api.post(`/projects/${projectId}/sprints/${sprintId}/tasks/${taskId}`).then(r => r.data),
};

// ── External Integrations ─────────────────────────────────────────────────────
export interface IntegrationConfig {
  id: number;
  projectId: number;
  provider: 'JIRA' | 'ASANA' | 'MONDAY' | 'GITHUB' | 'GITLAB' | 'MS_PROJECT';
  displayName: string;
  baseUrl?: string;
  status: 'PENDING' | 'CONNECTED' | 'ERROR' | 'DISCONNECTED';
  lastSyncedAt?: string;
  tokenPresent: boolean;
  configJson?: string;
}

export interface ConnectIntegrationRequest {
  provider: string;
  displayName?: string;
  baseUrl?: string;
  accessToken?: string;
  refreshToken?: string;
  configJson?: string;
}

export const integrationApi = {
  list:       (projectId: number) => api.get<IntegrationConfig[]>(`/projects/${projectId}/integrations`).then(r => r.data),
  connect:    (projectId: number, data: ConnectIntegrationRequest) => api.post<IntegrationConfig>(`/projects/${projectId}/integrations/connect`, data).then(r => r.data),
  sync:       (projectId: number, integrationId: number) => api.post<Record<string, unknown>>(`/projects/${projectId}/integrations/${integrationId}/sync`).then(r => r.data),
  disconnect: (projectId: number, integrationId: number) => api.delete<IntegrationConfig>(`/projects/${projectId}/integrations/${integrationId}`).then(r => r.data),
};

// ── Weekly Status Report ──────────────────────────────────────────────────────
export interface MilestoneSnapshot {
  name: string;
  targetDate: string;
  completedTaskCount: number;
  totalTaskCount: number;
  completionPct: number;
  status: string;
}

export interface WeeklyReport {
  projectId: number;
  projectName: string;
  methodology: string;
  generatedAt: string;
  reportDate: string;
  // Schedule
  actualProgressPct: number;
  expectedProgressPct: number;
  progressVariancePct: number;
  scheduleHealth: string;
  totalTasks: number;
  completedTasks: number;
  overdueTasks: number;
  blockedTasks: number;
  // Budget
  approvedBudget: number;
  actualCost: number;
  remainingBudget: number;
  budgetUsedPct: number;
  budgetHealth: string;
  // Team
  totalTeamMembers: number;
  overloadedMembers: number;
  avgUtilizationPct: number;
  // Risk
  openRisks: number;
  criticalRisks: number;
  delayProbabilityPct: number;
  overallRiskLevel: string;
  // Milestones
  milestones: MilestoneSnapshot[];
  // AI Narrative
  executiveSummary: string;
  keyAccomplishments: string[];
  activeBlockers: string[];
  nextStepRecommendations: string[];
  overallStatusColor: 'GREEN' | 'AMBER' | 'RED';
}

export const reportApi = {
  getWeekly: (projectId: number) => api.get<WeeklyReport>(`/projects/${projectId}/report/weekly`).then(r => r.data),
  downloadPdf: (projectId: number): string => `/api/v1/projects/${projectId}/report/pdf`,
};

export default api;



