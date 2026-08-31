package com.example.aiprojectmanager.risk.service;

import com.example.aiprojectmanager.task.domain.Task;
import com.example.aiprojectmanager.task.domain.TaskStatus;
import com.example.aiprojectmanager.task.repository.TaskRepository;
import com.example.aiprojectmanager.risk.domain.ProjectRisk;
import com.example.aiprojectmanager.risk.domain.RiskSeverity;
import com.example.aiprojectmanager.risk.domain.RiskType;
import com.example.aiprojectmanager.risk.repository.ProjectRiskRepository;
import com.example.aiprojectmanager.scheduling.dto.CriticalPathResponse;
import com.example.aiprojectmanager.scheduling.dto.GanttTaskItem;
import com.example.aiprojectmanager.scheduling.service.SchedulingService;
import com.example.aiprojectmanager.tracking.dto.BudgetHealthDTO;
import com.example.aiprojectmanager.tracking.dto.ProjectProgressDTO;
import com.example.aiprojectmanager.tracking.dto.ProjectWorkloadDTO;
import com.example.aiprojectmanager.tracking.dto.TeamMemberWorkloadDTO;
import com.example.aiprojectmanager.tracking.service.BudgetTrackingService;
import com.example.aiprojectmanager.tracking.service.ProgressTrackingService;
import com.example.aiprojectmanager.tracking.service.WorkloadAnalysisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RiskRuleEngine {

    private final ProjectRiskRepository riskRepository;
    private final TaskRepository taskRepository;
    private final ProgressTrackingService progressTrackingService;
    private final WorkloadAnalysisService workloadAnalysisService;
    private final BudgetTrackingService budgetTrackingService;
    private final SchedulingService schedulingService;
    private final ObjectMapper objectMapper;

    public void analyzeRisks(Long projectId, Long ownerId) {
        LocalDate today = LocalDate.now();
        List<Task> tasks = taskRepository.findByProjectId(projectId);
        
        ProjectProgressDTO progress = progressTrackingService.calculateProjectProgress(projectId, ownerId, today);
        ProjectWorkloadDTO workload = workloadAnalysisService.getProjectWorkload(projectId, ownerId);
        BudgetHealthDTO budget = budgetTrackingService.calculateBudgetHealth(projectId, ownerId, progress.getActualProgress());
        CriticalPathResponse cp = schedulingService.getCriticalPath(projectId);

        List<ProjectRisk> detectedRisks = new ArrayList<>();

        // A. Overdue task risk
        for (Task task : tasks) {
            if (task.getDueDate() != null && task.getDueDate().isBefore(today) && task.getStatus() != TaskStatus.DONE) {
                long daysOverdue = ChronoUnit.DAYS.between(task.getDueDate(), today);
                Map<String, Object> evidence = new HashMap<>();
                evidence.put("taskId", task.getId());
                evidence.put("taskTitle", task.getTitle());
                evidence.put("dueDate", task.getDueDate().toString());
                evidence.put("daysOverdue", daysOverdue);
                
                ProjectRisk risk = createRisk(projectId, RiskType.OVERDUE_TASK, 
                        daysOverdue > 7 ? RiskSeverity.HIGH : RiskSeverity.MEDIUM,
                        daysOverdue > 7 ? 75 : 50,
                        "Overdue Task: " + task.getTitle(),
                        "Task is overdue by " + daysOverdue + " days.",
                        evidence,
                        "Review task status, reassign if necessary, or adjust due dates.");
                detectedRisks.add(risk);
            }
        }

        // B. Critical-path delay risk
        for (com.example.aiprojectmanager.scheduling.dto.CriticalPathTaskItem item : cp.tasks()) {
            Task task = taskRepository.findById(item.id()).orElse(null);
            if (task != null) {
                if (task.getStatus() == TaskStatus.BLOCKED || 
                   (task.getDueDate() != null && task.getDueDate().isBefore(today) && task.getStatus() != TaskStatus.DONE)) {
                   
                    Map<String, Object> evidence = new HashMap<>();
                    evidence.put("taskId", task.getId());
                    evidence.put("taskTitle", task.getTitle());
                    evidence.put("status", task.getStatus().name());
                    
                    ProjectRisk risk = createRisk(projectId, RiskType.CRITICAL_PATH_DELAY, 
                            RiskSeverity.CRITICAL, 100,
                            "Critical Path Delayed: " + task.getTitle(),
                            "A task on the critical path is delayed or blocked, threatening the project completion date.",
                            evidence,
                            "Immediately resolve blockers for this task or allocate more resources to expedite it.");
                    detectedRisks.add(risk);
                }
            }
        }

        // C. Resource overload risk
        for (TeamMemberWorkloadDTO member : workload.getTeamWorkloads()) {
            if ("OVERLOADED".equals(member.getWorkloadStatus())) {
                Map<String, Object> evidence = new HashMap<>();
                evidence.put("memberId", member.getTeamMemberId());
                evidence.put("memberName", member.getTeamMemberName());
                evidence.put("utilizationPercentage", member.getUtilizationPercentage());
                evidence.put("assignedTaskCount", member.getAssignedTaskCount());
                
                ProjectRisk risk = createRisk(projectId, RiskType.RESOURCE_OVERLOAD, 
                        RiskSeverity.HIGH, 75,
                        "Resource Overloaded: " + member.getTeamMemberName(),
                        "Team member is overloaded with " + member.getUtilizationPercentage() + "% utilization.",
                        evidence,
                        "Reassign tasks to other team members with available capacity.");
                detectedRisks.add(risk);
            }
        }

        // D. Low-progress risk
        if ("OFF_TRACK".equals(progress.getProjectHealth())) {
            Map<String, Object> evidence = new HashMap<>();
            evidence.put("actualProgress", progress.getActualProgress());
            evidence.put("expectedProgress", progress.getExpectedProgress());
            evidence.put("variance", progress.getProgressVariance());
            
            ProjectRisk risk = createRisk(projectId, RiskType.LOW_PROGRESS, 
                    RiskSeverity.HIGH, 75,
                    "Low Project Progress",
                    "Project actual progress is significantly below expected progress.",
                    evidence,
                    "Review project schedule, increase resource allocation, or adjust scope.");
            detectedRisks.add(risk);
        }

        // E. Budget overrun risk
        if ("CRITICAL".equals(budget.getBudgetHealth()) || "HIGH".equals(budget.getBudgetHealth())) {
            Map<String, Object> evidence = new HashMap<>();
            evidence.put("approvedBudget", budget.getApprovedBudget());
            evidence.put("actualCost", budget.getActualCost());
            evidence.put("budgetUsedPercentage", budget.getBudgetUsedPercentage());
            
            RiskSeverity severity = "CRITICAL".equals(budget.getBudgetHealth()) ? RiskSeverity.CRITICAL : RiskSeverity.HIGH;
            ProjectRisk risk = createRisk(projectId, RiskType.BUDGET_OVERRUN, 
                    severity, severity == RiskSeverity.CRITICAL ? 100 : 75,
                    "Budget " + ("CRITICAL".equals(budget.getBudgetHealth()) ? "Overrun" : "At Risk"),
                    "Actual costs are exceeding the planned budget curve.",
                    evidence,
                    "Review expenses, cut non-essential costs, or request budget increase.");
            detectedRisks.add(risk);
        }

        // G. Blocked-task risk
        for (Task task : tasks) {
            if (task.getStatus() == TaskStatus.BLOCKED) {
                Map<String, Object> evidence = new HashMap<>();
                evidence.put("taskId", task.getId());
                evidence.put("taskTitle", task.getTitle());
                
                ProjectRisk risk = createRisk(projectId, RiskType.BLOCKED_TASK, 
                        RiskSeverity.MEDIUM, 50,
                        "Task Blocked: " + task.getTitle(),
                        "A task is currently blocked and cannot proceed.",
                        evidence,
                        "Investigate blockers and unblock the task as soon as possible.");
                detectedRisks.add(risk);
            }
        }

        // Save new or updated risks (Simple MVP: only insert if identical evidence/type doesn't exist)
        // More robust: matching by riskType and title
        for (ProjectRisk risk : detectedRisks) {
            // Very naive duplicate prevention for MVP
            List<ProjectRisk> existing = riskRepository.findByProjectIdOrderByDetectedAtDesc(projectId);
            boolean exists = existing.stream().anyMatch(r -> 
                r.getRiskType() == risk.getRiskType() && r.getTitle().equals(risk.getTitle()) && r.getStatus().name().equals("OPEN"));
            
            if (!exists) {
                riskRepository.save(risk);
            }
        }
    }

    private ProjectRisk createRisk(Long projectId, RiskType type, RiskSeverity severity, int score, 
                                   String title, String description, Map<String, Object> evidence, String suggestedAction) {
        ProjectRisk risk = new ProjectRisk();
        // Assume Project needs to be fetched or set by proxy, for simplicity here we assume we can just set project_id via a dummy Project entity
        com.example.aiprojectmanager.project.domain.Project p = new com.example.aiprojectmanager.project.domain.Project();
        p.setId(projectId);
        risk.setProject(p);
        
        risk.setRiskType(type);
        risk.setSeverity(severity);
        risk.setRiskScore(score);
        risk.setTitle(title);
        risk.setDescription(description);
        try {
            risk.setEvidenceJson(objectMapper.writeValueAsString(evidence));
        } catch (JsonProcessingException e) {
            risk.setEvidenceJson("{}");
        }
        risk.setSuggestedAction(suggestedAction);
        return risk;
    }
}
