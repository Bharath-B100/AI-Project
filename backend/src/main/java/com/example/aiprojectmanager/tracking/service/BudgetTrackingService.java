package com.example.aiprojectmanager.tracking.service;

import com.example.aiprojectmanager.project.domain.Project;
import com.example.aiprojectmanager.task.domain.Task;
import com.example.aiprojectmanager.project.repository.ProjectRepository;
import com.example.aiprojectmanager.task.repository.TaskRepository;
import com.example.aiprojectmanager.tracking.domain.ProjectCostEntry;
import com.example.aiprojectmanager.tracking.dto.BudgetHealthDTO;
import com.example.aiprojectmanager.tracking.dto.CreateCostEntryRequest;
import com.example.aiprojectmanager.tracking.repository.ProjectCostEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetTrackingService {

    private final ProjectCostEntryRepository costEntryRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final com.example.aiprojectmanager.assignment.repository.TaskAssignmentRepository taskAssignmentRepository;

    @Transactional
    public ProjectCostEntry addCostEntry(Long projectId, CreateCostEntryRequest request, Long ownerId) {
        Project project = projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));

        ProjectCostEntry entry = new ProjectCostEntry();
        entry.setProject(project);
        entry.setCategory(request.getCategory());
        entry.setDescription(request.getDescription());
        entry.setAmount(request.getAmount());
        entry.setEntryDate(request.getEntryDate());

        if (request.getTaskId() != null) {
            Task task = taskRepository.findById(request.getTaskId())
                    .orElseThrow(() -> new IllegalArgumentException("Task not found"));
            if (!task.getProjectId().equals(projectId)) {
                throw new IllegalArgumentException("Task does not belong to this project");
            }
            entry.setTask(task);
        }

        return costEntryRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<ProjectCostEntry> listCostEntries(Long projectId, Long ownerId) {
        projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));
        return costEntryRepository.findByProjectIdOrderByEntryDateDesc(projectId);
    }

    @Transactional(readOnly = true)
    public BudgetHealthDTO calculateBudgetHealth(Long projectId, Long ownerId, BigDecimal actualProjectProgress) {
        Project project = projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));

        List<ProjectCostEntry> entries = costEntryRepository.findByProjectIdOrderByEntryDateDesc(projectId);
        
        BigDecimal actualCost = BigDecimal.ZERO;
        for (ProjectCostEntry entry : entries) {
            actualCost = actualCost.add(entry.getAmount());
        }

        BigDecimal approvedBudget = project.getBudget() != null ? project.getBudget() : BigDecimal.ZERO;
        BigDecimal remainingBudget = approvedBudget.subtract(actualCost);
        
        BigDecimal budgetUsedPercentage = BigDecimal.ZERO;
        if (approvedBudget.compareTo(BigDecimal.ZERO) > 0) {
            budgetUsedPercentage = actualCost.divide(approvedBudget, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        }

        // Calculate estimated labor cost from assigned task planned hours * member hourly rate
        BigDecimal estimatedLaborCost = BigDecimal.ZERO;
        List<com.example.aiprojectmanager.assignment.domain.TaskAssignment> assignments = taskAssignmentRepository.findByProjectId(projectId);
        for (com.example.aiprojectmanager.assignment.domain.TaskAssignment ta : assignments) {
            if (ta.getPlannedHours() != null && ta.getTeamMember() != null && ta.getTeamMember().getHourlyRate() != null) {
                estimatedLaborCost = estimatedLaborCost.add(ta.getPlannedHours().multiply(ta.getTeamMember().getHourlyRate()));
            }
        }

        // Budget health rules based on milestone instructions
        // - LOW risk: budget used is below project completion percentage + 10%.
        // - MEDIUM risk: budget used exceeds project completion percentage by 10% to 20%.
        // - HIGH risk: budget used exceeds project completion percentage by more than 20%.
        // - CRITICAL risk: actual cost exceeds approved budget.
        
        String health = "LOW";
        if (approvedBudget.compareTo(BigDecimal.ZERO) == 0) {
            health = "LOW"; // No budget set
        } else if (actualCost.compareTo(approvedBudget) > 0) {
            health = "CRITICAL";
        } else {
             BigDecimal limitLow = actualProjectProgress.add(BigDecimal.valueOf(10));
             BigDecimal limitMedium = actualProjectProgress.add(BigDecimal.valueOf(20));
             
             if (budgetUsedPercentage.compareTo(limitMedium) > 0) {
                 health = "HIGH";
             } else if (budgetUsedPercentage.compareTo(limitLow) > 0) {
                 health = "MEDIUM";
             }
        }

        BudgetHealthDTO dto = new BudgetHealthDTO();
        dto.setApprovedBudget(approvedBudget);
        dto.setActualCost(actualCost);
        dto.setRemainingBudget(remainingBudget);
        dto.setBudgetUsedPercentage(budgetUsedPercentage);
        dto.setEstimatedLaborCost(estimatedLaborCost);
        dto.setBudgetHealth(health);

        return dto;
    }
}
