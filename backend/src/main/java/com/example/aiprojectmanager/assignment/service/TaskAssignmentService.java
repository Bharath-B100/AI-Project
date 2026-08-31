package com.example.aiprojectmanager.assignment.service;

import com.example.aiprojectmanager.assignment.domain.TaskAssignment;
import com.example.aiprojectmanager.assignment.dto.AssignTaskRequest;
import com.example.aiprojectmanager.assignment.dto.TaskAssignmentDTO;
import com.example.aiprojectmanager.assignment.repository.TaskAssignmentRepository;
import com.example.aiprojectmanager.project.domain.Project;
import com.example.aiprojectmanager.task.domain.Task;
import com.example.aiprojectmanager.project.repository.ProjectRepository;
import com.example.aiprojectmanager.task.repository.TaskRepository;
import com.example.aiprojectmanager.team.domain.TeamMember;
import com.example.aiprojectmanager.team.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskAssignmentService {
    
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final TaskRepository taskRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public TaskAssignmentDTO assignTask(Long taskId, AssignTaskRequest request, Long ownerId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
                
        Project project = projectRepository.findByIdAndOwnerId(task.getProjectId(), ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));

        TeamMember teamMember = teamMemberRepository.findByIdAndProjectId(request.getTeamMemberId(), project.getId())
                .orElseThrow(() -> new IllegalArgumentException("Team member not found in this project"));

        // Validate allocation percentage <= 100 for this task
        List<TaskAssignment> existingAssignments = taskAssignmentRepository.findByTaskId(taskId);
        BigDecimal currentAllocation = existingAssignments.stream()
                .filter(a -> !a.getTeamMember().getId().equals(teamMember.getId()))
                .map(a -> a.getAllocationPercentage())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        if (currentAllocation.add(request.getAllocationPercentage()).compareTo(BigDecimal.valueOf(100.0)) > 0) {
            throw new IllegalArgumentException("Total allocation for a task cannot exceed 100%");
        }

        TaskAssignment assignment = taskAssignmentRepository.findByTaskIdAndTeamMemberId(taskId, teamMember.getId())
                .orElse(new TaskAssignment());

        assignment.setTask(task);
        assignment.setTeamMember(teamMember);
        assignment.setAllocationPercentage(request.getAllocationPercentage());
        assignment.setPlannedHours(request.getPlannedHours());

        assignment = taskAssignmentRepository.save(assignment);
        return mapToDTO(assignment);
    }

    @Transactional(readOnly = true)
    public List<TaskAssignmentDTO> getTaskAssignments(Long taskId, Long ownerId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
                
        projectRepository.findByIdAndOwnerId(task.getProjectId(), ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));

        return taskAssignmentRepository.findByTaskId(taskId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void removeAssignment(Long taskId, Long assignmentId, Long ownerId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
                
        projectRepository.findByIdAndOwnerId(task.getProjectId(), ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied"));

        TaskAssignment assignment = taskAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));
                
        if (!assignment.getTask().getId().equals(taskId)) {
            throw new IllegalArgumentException("Assignment does not belong to this task");
        }

        taskAssignmentRepository.delete(assignment);
    }

    private TaskAssignmentDTO mapToDTO(TaskAssignment assignment) {
        TaskAssignmentDTO dto = new TaskAssignmentDTO();
        dto.setId(assignment.getId());
        dto.setTaskId(assignment.getTask().getId());
        dto.setTeamMemberId(assignment.getTeamMember().getId());
        dto.setTeamMemberName(assignment.getTeamMember().getName());
        dto.setAllocationPercentage(assignment.getAllocationPercentage());
        dto.setPlannedHours(assignment.getPlannedHours());
        dto.setActualHours(assignment.getActualHours());
        dto.setAssignedAt(assignment.getAssignedAt());
        return dto;
    }
}
