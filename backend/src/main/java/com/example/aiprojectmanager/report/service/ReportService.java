package com.example.aiprojectmanager.report.service;

import com.example.aiprojectmanager.common.NotFoundException;
import com.example.aiprojectmanager.project.domain.Project;
import com.example.aiprojectmanager.project.repository.ProjectRepository;
import com.example.aiprojectmanager.report.dto.StatusReportDTO;
import com.example.aiprojectmanager.risk.dto.PredictiveRiskDto;
import com.example.aiprojectmanager.risk.domain.ProjectRisk;
import com.example.aiprojectmanager.risk.repository.ProjectRiskRepository;
import com.example.aiprojectmanager.risk.service.PredictiveRiskService;
import com.example.aiprojectmanager.task.domain.Task;
import com.example.aiprojectmanager.task.domain.TaskStatus;
import com.example.aiprojectmanager.task.repository.TaskRepository;
import com.example.aiprojectmanager.tracking.dto.BudgetHealthDTO;
import com.example.aiprojectmanager.tracking.dto.ProjectProgressDTO;
import com.example.aiprojectmanager.tracking.dto.ProjectWorkloadDTO;
import com.example.aiprojectmanager.tracking.dto.TeamMemberWorkloadDTO;
import com.example.aiprojectmanager.tracking.service.BudgetTrackingService;
import com.example.aiprojectmanager.tracking.service.ProgressTrackingService;
import com.example.aiprojectmanager.tracking.service.WorkloadAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregates all project health signals into a structured weekly status report
 * with AI-generated narrative text.  No external LLM call — narrative is built
 * from deterministic rules, keeping latency near-zero and the service free of
 * API-key dependencies for an academic demo.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ProjectRepository        projectRepository;
    private final TaskRepository           taskRepository;
    private final ProjectRiskRepository    riskRepository;
    private final ProgressTrackingService  progressService;
    private final BudgetTrackingService    budgetService;
    private final WorkloadAnalysisService  workloadService;
    private final PredictiveRiskService    predictiveRiskService;

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    public StatusReportDTO generateWeeklyReport(Long projectId, Long ownerId) {
        Project project = projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new NotFoundException("Project not found or access denied"));

        LocalDate today = LocalDate.now();

        // 1. Gather all sub-reports
        ProjectProgressDTO progress = safeProgress(projectId, ownerId, today);
        BudgetHealthDTO    budget   = safeBudget(projectId, ownerId, progress);
        ProjectWorkloadDTO workload = safeWorkload(projectId, ownerId);
        PredictiveRiskDto  predict  = safePredict(projectId, ownerId);
        List<ProjectRisk>  risks    = riskRepository.findByProjectIdOrderByDetectedAtDesc(projectId);

        // 2. Task breakdown
        List<Task> tasks        = taskRepository.findByProjectId(projectId);
        int blocked             = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.BLOCKED).count();
        int open                = (int) risks.stream().filter(r -> "OPEN".equalsIgnoreCase(r.getStatus().name())).count();
        int critical            = (int) risks.stream().filter(r -> "CRITICAL".equalsIgnoreCase(r.getSeverity().name())).count();

        // 3. Workload aggregation
        List<TeamMemberWorkloadDTO> wl    = workload != null ? workload.getTeamWorkloads() : Collections.emptyList();
        int overloaded                    = (int) wl.stream().filter(m -> "OVERLOADED".equals(m.getWorkloadStatus())).count();
        double avgUtil                    = wl.isEmpty() ? 0 : wl.stream()
                .mapToDouble(m -> m.getUtilizationPercentage() != null ? m.getUtilizationPercentage().doubleValue() : 0)
                .average().orElse(0);

        // 4. Milestone snapshots
        List<StatusReportDTO.MilestoneSnapshot> milestones = buildMilestoneSnapshots(tasks, project);

        // 5. Overall color
        String health   = progress != null ? progress.getProjectHealth() : "ON_TRACK";
        String budgetH  = budget   != null ? budget.getBudgetHealth()    : "LOW";
        String color    = deriveStatusColor(health, budgetH, open, critical);

        // 6. Narrative
        double actualPct   = progress != null ? progress.getActualProgress().doubleValue()   : 0;
        double expectedPct = progress != null ? progress.getExpectedProgress().doubleValue() : 0;
        double delayProb   = predict  != null ? predict.getDelayProbabilityPercentage()      : 0;
        double budgetUsed  = budget   != null ? budget.getBudgetUsedPercentage().doubleValue(): 0;

        String summary          = buildExecutiveSummary(project, actualPct, expectedPct, budgetUsed, delayProb, color);
        List<String> accomplishments = buildAccomplishments(progress, tasks);
        List<String> blockers        = buildBlockers(tasks, risks, overloaded, budgetH);
        List<String> next            = buildNextSteps(health, budgetH, predict, overloaded);

        return StatusReportDTO.builder()
                .projectId(projectId)
                .projectName(project.getName())
                .methodology(project.getMethodology() != null ? project.getMethodology() : "Agile")
                .generatedAt(LocalDateTime.now())
                .reportDate(today)
                // schedule
                .actualProgressPct(actualPct)
                .expectedProgressPct(expectedPct)
                .progressVariancePct(progress != null ? progress.getProgressVariance().doubleValue() : 0)
                .scheduleHealth(health)
                .totalTasks(progress != null ? progress.getTotalTasks() : tasks.size())
                .completedTasks(progress != null ? progress.getCompletedTasks() : 0)
                .overdueTasks(progress != null ? progress.getOverdueTasks() : 0)
                .blockedTasks(blocked)
                // budget
                .approvedBudget(budget != null ? budget.getApprovedBudget().doubleValue()   : 0)
                .actualCost(budget    != null ? budget.getActualCost().doubleValue()        : 0)
                .remainingBudget(budget != null ? budget.getRemainingBudget().doubleValue() : 0)
                .budgetUsedPct(budgetUsed)
                .budgetHealth(budgetH)
                // team
                .totalTeamMembers(wl.size())
                .overloadedMembers(overloaded)
                .avgUtilizationPct(Math.round(avgUtil * 10.0) / 10.0)
                // risk
                .openRisks(open)
                .criticalRisks(critical)
                .delayProbabilityPct(delayProb)
                .overallRiskLevel(predict != null ? predict.getRiskLevel() : "LOW")
                // milestones
                .milestones(milestones)
                // narrative
                .executiveSummary(summary)
                .keyAccomplishments(accomplishments)
                .activeBlockers(blockers)
                .nextStepRecommendations(next)
                .overallStatusColor(color)
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Narrative builders
    // ──────────────────────────────────────────────────────────────────────────

    private String buildExecutiveSummary(Project project, double actual, double expected,
                                         double budgetUsed, double delayProb, String color) {
        String trend = actual >= expected ? "ahead of" : "behind";
        double gap   = Math.abs(actual - expected);
        String risk  = delayProb > 50 ? String.format(" Monte Carlo analysis indicates a %.0f%% probability of schedule slippage — immediate corrective action is advised.", delayProb)
                     : delayProb > 25 ? String.format(" Probabilistic forecasting places delay risk at %.0f%%, warranting close monitoring.", delayProb)
                     : " Probabilistic schedule risk remains within acceptable thresholds.";
        return String.format(
            "Project \"%s\" is currently %.1f%% complete against an expected %.1f%% — %.1f%% %s schedule.%s " +
            "Budget utilisation stands at %.1f%%. Overall project status is %s.",
            project.getName(), actual, expected, gap, trend, risk, budgetUsed,
            color.equals("GREEN") ? "GREEN (healthy)" : color.equals("AMBER") ? "AMBER (at risk)" : "RED (critical)"
        );
    }

    private List<String> buildAccomplishments(ProjectProgressDTO progress, List<Task> tasks) {
        List<String> list = new ArrayList<>();
        if (progress != null && progress.getCompletedTasks() > 0) {
            list.add(String.format("%d task(s) completed this period, contributing to overall project progress.", progress.getCompletedTasks()));
        }
        long inProgress = tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
        if (inProgress > 0) list.add(String.format("%d task(s) currently in active development.", inProgress));
        if (list.isEmpty()) list.add("Project initialised; task execution has not yet commenced.");
        return list;
    }

    private List<String> buildBlockers(List<Task> tasks, List<ProjectRisk> risks, int overloaded, String budgetH) {
        List<String> list = new ArrayList<>();
        long blocked = tasks.stream().filter(t -> t.getStatus() == TaskStatus.BLOCKED).count();
        if (blocked > 0) list.add(String.format("%d task(s) in BLOCKED state — immediate dependency resolution required.", blocked));
        if (overloaded > 0) list.add(String.format("%d team member(s) are overloaded — workload rebalancing recommended.", overloaded));
        if ("CRITICAL".equals(budgetH)) list.add("Actual cost has exceeded the approved budget — escalation required.");
        else if ("HIGH".equals(budgetH))  list.add("Budget burn rate significantly exceeds project completion percentage.");
        risks.stream()
             .filter(r -> "CRITICAL".equalsIgnoreCase(r.getSeverity().name()) && "OPEN".equalsIgnoreCase(r.getStatus().name()))
             .limit(3)
             .forEach(r -> list.add("CRITICAL Risk: " + r.getTitle()));
        if (list.isEmpty()) list.add("No active blockers identified at this time.");
        return list;
    }

    private List<String> buildNextSteps(String health, String budgetH, PredictiveRiskDto predict, int overloaded) {
        List<String> list = new ArrayList<>();
        if ("OFF_TRACK".equals(health))  list.add("Run What-If simulation to assess fast-tracking or resource augmentation options.");
        if ("AT_RISK".equals(health))    list.add("Review upcoming milestone dependencies and adjust task scheduling buffers.");
        if (overloaded > 0)              list.add("Apply 1-click resource levelling to redistribute tasks from overloaded members.");
        if ("HIGH".equals(budgetH) || "CRITICAL".equals(budgetH)) list.add("Review non-critical expenditures and re-baseline the cost plan.");
        if (predict != null && predict.getDelayProbabilityPercentage() > 40)
            list.add("Address top risk drivers: " + String.join("; ", predict.getTopRiskDrivers()).substring(0, Math.min(120, String.join("; ", predict.getTopRiskDrivers()).length())) + ".");
        list.add("Conduct weekly team sync to verify task statuses and unblock dependencies.");
        return list;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Milestone snapshots (group tasks by milestone/dueDate bucket)
    // ──────────────────────────────────────────────────────────────────────────

    private List<StatusReportDTO.MilestoneSnapshot> buildMilestoneSnapshots(List<Task> tasks, Project project) {
        // Group tasks by dueDate month as a proxy for milestone
        Map<String, List<Task>> byMonth = tasks.stream()
                .filter(t -> t.getDueDate() != null)
                .collect(Collectors.groupingBy(t -> t.getDueDate().getYear() + "-" +
                        String.format("%02d", t.getDueDate().getMonthValue())));

        List<StatusReportDTO.MilestoneSnapshot> snaps = new ArrayList<>();
        for (Map.Entry<String, List<Task>> e : new TreeMap<>(byMonth).entrySet()) {
            List<Task> group = e.getValue();
            int done  = (int) group.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
            int total = group.size();
            double pct = total > 0 ? (done * 100.0 / total) : 0;
            LocalDate target = group.stream().map(Task::getDueDate).filter(Objects::nonNull)
                    .max(Comparator.naturalOrder()).orElse(LocalDate.now());
            String status = pct >= 100 ? "COMPLETED" : LocalDate.now().isAfter(target) ? "MISSED" : pct >= 50 ? "ON_TRACK" : "AT_RISK";
            snaps.add(StatusReportDTO.MilestoneSnapshot.builder()
                    .name("Milestone " + e.getKey())
                    .targetDate(target)
                    .completedTaskCount(done)
                    .totalTaskCount(total)
                    .completionPct(Math.round(pct * 10.0) / 10.0)
                    .status(status)
                    .build());
        }

        // Add overall project as final milestone if project has end date
        if (project.getEndDate() != null && !tasks.isEmpty()) {
            int done  = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
            double pct = tasks.isEmpty() ? 0 : (done * 100.0 / tasks.size());
            snaps.add(StatusReportDTO.MilestoneSnapshot.builder()
                    .name("Project Completion")
                    .targetDate(project.getEndDate())
                    .completedTaskCount(done)
                    .totalTaskCount(tasks.size())
                    .completionPct(Math.round(pct * 10.0) / 10.0)
                    .status(pct >= 100 ? "COMPLETED" : LocalDate.now().isAfter(project.getEndDate()) ? "MISSED" : "ON_TRACK")
                    .build());
        }
        return snaps;
    }

    private String deriveStatusColor(String health, String budgetH, int openRisks, int criticalRisks) {
        if ("OFF_TRACK".equals(health) || "CRITICAL".equals(budgetH) || criticalRisks >= 2) return "RED";
        if ("AT_RISK".equals(health)   || "HIGH".equals(budgetH)     || openRisks >= 3)     return "AMBER";
        return "GREEN";
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Safe delegators (return null instead of throwing on empty projects)
    // ──────────────────────────────────────────────────────────────────────────

    private ProjectProgressDTO safeProgress(Long pid, Long uid, LocalDate today) {
        try { return progressService.calculateProjectProgress(pid, uid, today); } catch (Exception e) { return null; }
    }

    private BudgetHealthDTO safeBudget(Long pid, Long uid, ProjectProgressDTO p) {
        try {
            BigDecimal actual = p != null ? p.getActualProgress() : BigDecimal.ZERO;
            return budgetService.calculateBudgetHealth(pid, uid, actual);
        } catch (Exception e) { return null; }
    }

    private ProjectWorkloadDTO safeWorkload(Long pid, Long uid) {
        try { return workloadService.getProjectWorkload(pid, uid); } catch (Exception e) { return null; }
    }

    private PredictiveRiskDto safePredict(Long pid, Long uid) {
        try { return predictiveRiskService.evaluatePredictiveRisk(pid, uid); } catch (Exception e) { return null; }
    }
}
