package com.example.aiprojectmanager.tracking.service;

import com.example.aiprojectmanager.project.domain.Project;
import com.example.aiprojectmanager.project.domain.ProjectStatus;
import com.example.aiprojectmanager.task.domain.Task;
import com.example.aiprojectmanager.task.domain.TaskStatus;
import com.example.aiprojectmanager.project.repository.ProjectRepository;
import com.example.aiprojectmanager.task.repository.TaskRepository;
import com.example.aiprojectmanager.tracking.dto.ProjectProgressDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgressTrackingService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public ProjectProgressDTO calculateProjectProgress(Long projectId, Long ownerId, LocalDate currentDate) {
        Project project = projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));

        List<Task> tasks = taskRepository.findByProjectId(projectId);

        BigDecimal totalWeightedProgress = BigDecimal.ZERO;
        BigDecimal totalEstimatedHours = BigDecimal.ZERO;

        int completedTasks = 0;
        int overdueTasks = 0;

        for (Task task : tasks) {
            BigDecimal estHours = task.getEstimatedHours() != null ? task.getEstimatedHours() : BigDecimal.ONE; // default weight 1 if not set
            totalEstimatedHours = totalEstimatedHours.add(estHours);

            int progressPercentage = task.getProgressPercentage();
            if (task.getStatus() == TaskStatus.DONE) {
                progressPercentage = 100;
            } else if (task.getStatus() == TaskStatus.TODO) {
                progressPercentage = 0;
            }

            BigDecimal taskWeight = estHours.multiply(BigDecimal.valueOf(progressPercentage));
            totalWeightedProgress = totalWeightedProgress.add(taskWeight);

            if (task.getStatus() == TaskStatus.DONE) {
                completedTasks++;
            }
            if (task.getDueDate() != null && task.getDueDate().isBefore(currentDate) && task.getStatus() != TaskStatus.DONE) {
                overdueTasks++;
            }
        }

        BigDecimal actualProgress = BigDecimal.ZERO;
        if (totalEstimatedHours.compareTo(BigDecimal.ZERO) > 0) {
            actualProgress = totalWeightedProgress.divide(totalEstimatedHours, 2, RoundingMode.HALF_UP);
        }

        BigDecimal expectedProgress = calculateExpectedProgress(project, currentDate);
        BigDecimal progressVariance = actualProgress.subtract(expectedProgress);

        String health = "ON_TRACK";
        if (progressVariance.compareTo(BigDecimal.valueOf(-15)) <= 0) {
            health = "OFF_TRACK";
        } else if (progressVariance.compareTo(BigDecimal.valueOf(-5)) <= 0) {
            health = "AT_RISK";
        }

        ProjectProgressDTO dto = new ProjectProgressDTO();
        dto.setActualProgress(actualProgress);
        dto.setExpectedProgress(expectedProgress);
        dto.setProgressVariance(progressVariance);
        dto.setTotalTasks(tasks.size());
        dto.setCompletedTasks(completedTasks);
        dto.setOverdueTasks(overdueTasks);
        dto.setProjectHealth(health);

        return dto;
    }

    private BigDecimal calculateExpectedProgress(Project project, LocalDate currentDate) {
        if (project.getStartDate() == null || project.getEndDate() == null) {
            return BigDecimal.ZERO;
        }

        if (currentDate.isBefore(project.getStartDate())) {
            return BigDecimal.ZERO;
        }
        if (currentDate.isAfter(project.getEndDate()) || currentDate.isEqual(project.getEndDate())) {
            return BigDecimal.valueOf(100.00);
        }

        long totalDays = ChronoUnit.DAYS.between(project.getStartDate(), project.getEndDate());
        if (totalDays <= 0) return BigDecimal.valueOf(100.00);

        long elapsedDays = ChronoUnit.DAYS.between(project.getStartDate(), currentDate);
        
        BigDecimal expected = BigDecimal.valueOf(elapsedDays)
                .divide(BigDecimal.valueOf(totalDays), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
                
        return expected.setScale(2, RoundingMode.HALF_UP);
    }
}
