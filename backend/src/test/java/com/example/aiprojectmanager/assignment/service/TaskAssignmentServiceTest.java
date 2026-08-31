package com.example.aiprojectmanager.assignment.service;

import com.example.aiprojectmanager.assignment.domain.TaskAssignment;
import com.example.aiprojectmanager.assignment.dto.AssignTaskRequest;
import com.example.aiprojectmanager.assignment.dto.TaskAssignmentDTO;
import com.example.aiprojectmanager.assignment.repository.TaskAssignmentRepository;
import com.example.aiprojectmanager.project.domain.Project;
import com.example.aiprojectmanager.project.repository.ProjectRepository;
import com.example.aiprojectmanager.task.domain.Task;
import com.example.aiprojectmanager.task.repository.TaskRepository;
import com.example.aiprojectmanager.team.domain.TeamMember;
import com.example.aiprojectmanager.team.repository.TeamMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskAssignmentServiceTest {

    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private TaskAssignmentService taskAssignmentService;

    private Long ownerId = 1L;
    private Project project;
    private Task task;
    private TeamMember member;

    @BeforeEach
    void setup() {
        project = new Project();
        project.setId(10L);
        project.setOwnerId(ownerId);
        project.setBudget(BigDecimal.valueOf(1000));

        task = new Task();
        task.setId(100L);
        task.setProjectId(10L);

        member = new TeamMember();
        member.setId(5L);
        member.setProject(project);
        member.setHourlyRate(BigDecimal.valueOf(50));
    }

    @Test
    void testAssignTask_Success() {
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(projectRepository.findByIdAndOwnerId(10L, ownerId)).thenReturn(Optional.of(project));
        when(teamMemberRepository.findByIdAndProjectId(5L, 10L)).thenReturn(Optional.of(member));
        when(taskAssignmentRepository.findByTaskId(100L)).thenReturn(Collections.emptyList());
        when(taskAssignmentRepository.findByTaskIdAndTeamMemberId(100L, 5L)).thenReturn(Optional.empty());

        when(taskAssignmentRepository.save(any(TaskAssignment.class))).thenAnswer(invocation -> {
            TaskAssignment ta = invocation.getArgument(0);
            ta.setId(1L);
            return ta;
        });

        AssignTaskRequest request = new AssignTaskRequest();
        request.setTeamMemberId(5L);
        request.setAllocationPercentage(BigDecimal.valueOf(50));
        request.setPlannedHours(BigDecimal.valueOf(10));

        TaskAssignmentDTO dto = taskAssignmentService.assignTask(100L, request, ownerId);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(5L, dto.getTeamMemberId());
        assertEquals(BigDecimal.valueOf(50), dto.getAllocationPercentage());
        assertEquals(BigDecimal.valueOf(10), dto.getPlannedHours());
    }
}
