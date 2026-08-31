package com.example.aiprojectmanager.report.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Weekly executive status report aggregating all project health dimensions.
 * Designed to be used both for in-app display and as the data model for PDF export.
 */
@Data
@Builder
public class StatusReportDTO {

    // ── Identity ───────────────────────────────────────────────────────────────
    private Long   projectId;
    private String projectName;
    private String methodology;
    private LocalDateTime generatedAt;
    private LocalDate     reportDate;

    // ── Schedule Health ────────────────────────────────────────────────────────
    private double actualProgressPct;
    private double expectedProgressPct;
    private double progressVariancePct;
    private String scheduleHealth;          // ON_TRACK | AT_RISK | OFF_TRACK
    private int    totalTasks;
    private int    completedTasks;
    private int    overdueTasks;
    private int    blockedTasks;

    // ── Budget Health ──────────────────────────────────────────────────────────
    private double approvedBudget;
    private double actualCost;
    private double remainingBudget;
    private double budgetUsedPct;
    private String budgetHealth;            // LOW | MEDIUM | HIGH | CRITICAL

    // ── Team / Workload ────────────────────────────────────────────────────────
    private int    totalTeamMembers;
    private int    overloadedMembers;
    private double avgUtilizationPct;

    // ── Risk Summary ───────────────────────────────────────────────────────────
    private int    openRisks;
    private int    criticalRisks;
    private double delayProbabilityPct;     // from Monte Carlo
    private String overallRiskLevel;        // LOW | MODERATE | HIGH | CRITICAL

    // ── Milestone Snapshot ─────────────────────────────────────────────────────
    private List<MilestoneSnapshot> milestones;

    // ── AI-Generated Narrative ─────────────────────────────────────────────────
    private String executiveSummary;        // 3–4 sentence AI narrative
    private List<String> keyAccomplishments;
    private List<String> activeBlockers;
    private List<String> nextStepRecommendations;
    private String overallStatusColor;      // GREEN | AMBER | RED

    @Data
    @Builder
    public static class MilestoneSnapshot {
        private String name;
        private LocalDate targetDate;
        private int    completedTaskCount;
        private int    totalTaskCount;
        private double completionPct;
        private String status;              // ON_TRACK | AT_RISK | MISSED
    }
}
