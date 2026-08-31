package com.example.aiprojectmanager.project.service;

import com.example.aiprojectmanager.common.NotFoundException;
import com.example.aiprojectmanager.project.domain.Project;
import com.example.aiprojectmanager.project.domain.Sprint;
import com.example.aiprojectmanager.project.dto.SprintDTO;
import com.example.aiprojectmanager.project.repository.ProjectRepository;
import com.example.aiprojectmanager.project.repository.SprintRepository;
import com.example.aiprojectmanager.task.domain.Task;
import com.example.aiprojectmanager.task.domain.TaskStatus;
import com.example.aiprojectmanager.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SprintService {

    private final SprintRepository   sprintRepository;
    private final ProjectRepository  projectRepository;
    private final TaskRepository     taskRepository;

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SprintDTO> listSprints(Long projectId, Long ownerId) {
        verifyOwner(projectId, ownerId);
        return sprintRepository.findByProjectIdOrderByStartDateAsc(projectId)
                .stream().map(s -> toDTO(s, projectId)).collect(Collectors.toList());
    }

    @Transactional
    public SprintDTO createSprint(Long projectId, Long ownerId, SprintDTO req) {
        Project project = verifyOwner(projectId, ownerId);
        Sprint sprint = Sprint.builder()
                .project(project)
                .name(req.getName())
                .goal(req.getGoal())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .status("PLANNED")
                .velocityPoints(req.getVelocityPoints() != null ? req.getVelocityPoints() : 0)
                .build();
        return toDTO(sprintRepository.save(sprint), projectId);
    }

    @Transactional
    public SprintDTO startSprint(Long projectId, Long sprintId, Long ownerId) {
        verifyOwner(projectId, ownerId);
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new NotFoundException("Sprint not found"));
        if (!"PLANNED".equals(sprint.getStatus()))
            throw new IllegalStateException("Only PLANNED sprints can be started");
        sprint.setStatus("ACTIVE");
        sprint.setUpdatedAt(LocalDateTime.now());
        return toDTO(sprintRepository.save(sprint), projectId);
    }

    @Transactional
    public SprintDTO completeSprint(Long projectId, Long sprintId, Long ownerId) {
        verifyOwner(projectId, ownerId);
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new NotFoundException("Sprint not found"));
        if (!"ACTIVE".equals(sprint.getStatus()))
            throw new IllegalStateException("Only ACTIVE sprints can be completed");

        // Compute actual velocity from completed story points
        List<Task> sprintTasks = taskRepository.findBySprintIdAndProjectId(sprintId, projectId);
        int velocity = sprintTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .mapToInt(t -> t.getStoryPoints() != null ? t.getStoryPoints() : 0)
                .sum();
        sprint.setVelocityPoints(velocity);
        sprint.setStatus("COMPLETED");
        sprint.setUpdatedAt(LocalDateTime.now());

        // Move incomplete tasks to backlog
        sprintTasks.stream()
                .filter(t -> t.getStatus() != TaskStatus.DONE)
                .forEach(t -> { t.setSprintId(null); taskRepository.save(t); });

        return toDTO(sprintRepository.save(sprint), projectId);
    }

    @Transactional
    public void assignTaskToSprint(Long projectId, Long sprintId, Long taskId, Long ownerId) {
        verifyOwner(projectId, ownerId);
        if (!sprintRepository.existsByProjectIdAndId(projectId, sprintId))
            throw new NotFoundException("Sprint not found in this project");
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        if (!task.getProjectId().equals(projectId))
            throw new IllegalArgumentException("Task does not belong to this project");
        task.setSprintId(sprintId);
        taskRepository.save(task);
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private SprintDTO toDTO(Sprint s, Long projectId) {
        List<Task> tasks = taskRepository.findBySprintIdAndProjectId(s.getId(), projectId);
        int done  = (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
        int total = tasks.size();
        int totalSP = tasks.stream().mapToInt(t -> t.getStoryPoints() != null ? t.getStoryPoints() : 0).sum();
        int doneSP  = tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE)
                           .mapToInt(t -> t.getStoryPoints() != null ? t.getStoryPoints() : 0).sum();

        SprintDTO dto = new SprintDTO();
        dto.setId(s.getId());
        dto.setProjectId(s.getProject().getId());
        dto.setName(s.getName());
        dto.setGoal(s.getGoal());
        dto.setStartDate(s.getStartDate());
        dto.setEndDate(s.getEndDate());
        dto.setStatus(s.getStatus());
        dto.setVelocityPoints(s.getVelocityPoints());
        dto.setTotalTasks(total);
        dto.setCompletedTasks(done);
        dto.setTotalStoryPoints(totalSP);
        dto.setCompletedStoryPoints(doneSP);
        dto.setCompletionPct(total > 0 ? Math.round(done * 1000.0 / total) / 10.0 : 0);
        return dto;
    }

    private Project verifyOwner(Long projectId, Long ownerId) {
        return projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new NotFoundException("Project not found or access denied"));
    }
}
