package com.example.aiprojectmanager.risk.service;

import com.example.aiprojectmanager.risk.domain.ProjectRisk;
import com.example.aiprojectmanager.risk.repository.ProjectRiskRepository;
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
import com.example.aiprojectmanager.scheduling.service.SchedulingService;
import com.example.aiprojectmanager.scheduling.dto.CriticalPathResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RiskRuleEngineTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ProjectRiskRepository riskRepository;
    @Mock
    private ProgressTrackingService progressTrackingService;
    @Mock
    private WorkloadAnalysisService workloadAnalysisService;
    @Mock
    private BudgetTrackingService budgetTrackingService;
    @Mock
    private SchedulingService schedulingService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RiskRuleEngine riskRuleEngine;

    private Long projectId = 1L;
    private Long ownerId = 100L;

    @BeforeEach
    void setup() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        ProjectProgressDTO progress = new ProjectProgressDTO();
        progress.setActualProgress(BigDecimal.valueOf(50));
        progress.setExpectedProgress(BigDecimal.valueOf(60));
        progress.setProjectHealth("OFF_TRACK"); // Triggers Low-progress risk

        when(progressTrackingService.calculateProjectProgress(eq(projectId), eq(ownerId), any(LocalDate.class)))
                .thenReturn(progress);

        ProjectWorkloadDTO workload = new ProjectWorkloadDTO();
        TeamMemberWorkloadDTO memberWorkload = new TeamMemberWorkloadDTO();
        memberWorkload.setTeamMemberId(2L);
        memberWorkload.setTeamMemberName("John Doe");
        memberWorkload.setAssignedTaskCount(5);
        memberWorkload.setPlannedHours(BigDecimal.valueOf(50));
        memberWorkload.setUtilizationPercentage(BigDecimal.valueOf(125));
        memberWorkload.setWorkloadStatus("OVERLOADED"); // Triggers workload risk
        workload.setTeamWorkloads(List.of(memberWorkload));
        when(workloadAnalysisService.getProjectWorkload(projectId, ownerId)).thenReturn(workload);

        BudgetHealthDTO budget = new BudgetHealthDTO();
        budget.setApprovedBudget(BigDecimal.valueOf(1000));
        budget.setActualCost(BigDecimal.valueOf(900));
        budget.setEstimatedLaborCost(BigDecimal.valueOf(600));
        budget.setBudgetHealth("CRITICAL"); // Triggers budget overrun risk
        when(budgetTrackingService.calculateBudgetHealth(eq(projectId), eq(ownerId), any(BigDecimal.class)))
                .thenReturn(budget);

        CriticalPathResponse cp = new CriticalPathResponse(Collections.emptyList(), 10);
        when(schedulingService.getCriticalPath(projectId)).thenReturn(cp);
    }

    @Test
    void testAnalyzeRisks_DetectsAllRisks() {
        Task task = new Task();
        task.setId(1L);
        task.setTitle("Overdue Task");
        task.setStatus(TaskStatus.TODO);
        task.setDueDate(LocalDate.now().minusDays(5)); // Overdue by 5 days
        
        when(taskRepository.findByProjectId(projectId)).thenReturn(List.of(task));

        riskRuleEngine.analyzeRisks(projectId, ownerId);

        ArgumentCaptor<ProjectRisk> captor = ArgumentCaptor.forClass(ProjectRisk.class);
        verify(riskRepository, times(4)).save(captor.capture());

        List<ProjectRisk> savedRisks = captor.getAllValues();
        // Should detect: 1 overdue task risk, 1 schedule variance risk, 1 overload risk, 1 budget risk
        assertEquals(4, savedRisks.size());
    }
}
