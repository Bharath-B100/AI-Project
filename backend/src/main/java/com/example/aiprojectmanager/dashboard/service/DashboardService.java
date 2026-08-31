package com.example.aiprojectmanager.dashboard.service;

import com.example.aiprojectmanager.auth.CurrentUserService;
import com.example.aiprojectmanager.dashboard.dto.DashboardOverviewDTO;
import com.example.aiprojectmanager.project.domain.Project;
import com.example.aiprojectmanager.project.domain.ProjectStatus;
import com.example.aiprojectmanager.project.repository.ProjectRepository;
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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProjectRepository projectRepository;
    private final ProgressTrackingService progressTrackingService;
    private final BudgetTrackingService budgetTrackingService;
    private final WorkloadAnalysisService workloadAnalysisService;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public DashboardOverviewDTO getDashboardOverview() {
        Long ownerId = currentUserService.getCurrentUserId();
        List<Project> allProjects = projectRepository.findAllByOwnerIdOrderByUpdatedAtDesc(ownerId);
        
        DashboardOverviewDTO overview = new DashboardOverviewDTO();
        overview.setTotalProjects(allProjects.size());
        
        int activeCount = 0;
        BigDecimal totalBudget = BigDecimal.ZERO;
        BigDecimal totalSpent = BigDecimal.ZERO;
        
        boolean hasOffTrack = false;
        boolean hasAtRisk = false;
        boolean hasOverloaded = false;
        boolean hasNearCapacity = false;
        
        LocalDate currentDate = LocalDate.now();

        for (Project project : allProjects) {
            if (project.getStatus() == ProjectStatus.ACTIVE) {
                activeCount++;
            }

            BudgetHealthDTO budget = budgetTrackingService.calculateBudgetHealth(project.getId(), ownerId, BigDecimal.ZERO);
            totalBudget = totalBudget.add(budget.getApprovedBudget());
            totalSpent = totalSpent.add(budget.getActualCost());

            ProjectProgressDTO progress = progressTrackingService.calculateProjectProgress(project.getId(), ownerId, currentDate);
            if ("OFF_TRACK".equals(progress.getProjectHealth())) {
                hasOffTrack = true;
            } else if ("AT_RISK".equals(progress.getProjectHealth())) {
                hasAtRisk = true;
            }

            ProjectWorkloadDTO workload = workloadAnalysisService.getProjectWorkload(project.getId(), ownerId);
            for (TeamMemberWorkloadDTO tm : workload.getTeamWorkloads()) {
                if ("OVERLOADED".equals(tm.getWorkloadStatus())) {
                    hasOverloaded = true;
                } else if ("NEAR_CAPACITY".equals(tm.getWorkloadStatus())) {
                    hasNearCapacity = true;
                }
            }
        }

        overview.setActiveProjects(activeCount);
        overview.setTotalBudget(totalBudget);
        overview.setTotalSpent(totalSpent);
        
        if (allProjects.isEmpty()) {
            overview.setGlobalHealthStatus("IDLE");
            overview.setGlobalWorkloadStatus("IDLE");
        } else {
            if (hasOffTrack) overview.setGlobalHealthStatus("OFF_TRACK");
            else if (hasAtRisk) overview.setGlobalHealthStatus("AT_RISK");
            else overview.setGlobalHealthStatus("ON_TRACK");

            if (hasOverloaded) overview.setGlobalWorkloadStatus("OVERLOADED");
            else if (hasNearCapacity) overview.setGlobalWorkloadStatus("NEAR_CAPACITY");
            else overview.setGlobalWorkloadStatus("AVAILABLE");
        }

        List<Project> recentProjects = allProjects.stream()
                .sorted(Comparator.comparing(Project::getId).reversed())
                .limit(4)
                .collect(Collectors.toList());
        overview.setRecentProjects(recentProjects);

        return overview;
    }
}
