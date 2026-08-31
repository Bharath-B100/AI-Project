package com.example.aiprojectmanager.project.service;

import com.example.aiprojectmanager.auth.CurrentUserService;
import com.example.aiprojectmanager.common.*;
import com.example.aiprojectmanager.project.domain.*;
import com.example.aiprojectmanager.project.dto.*;
import com.example.aiprojectmanager.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

import com.example.aiprojectmanager.task.repository.TaskRepository;
import com.example.aiprojectmanager.team.repository.TeamMemberRepository;
import com.example.aiprojectmanager.risk.repository.ProjectRiskRepository;
import com.example.aiprojectmanager.scheduling.repository.TaskDependencyRepository;
import com.example.aiprojectmanager.assignment.repository.TaskAssignmentRepository;

@Service
@Transactional
public class ProjectService {
    private final ProjectRepository projects;
    private final CurrentUserService currentUserService;
    private final TaskRepository taskRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ProjectRiskRepository projectRiskRepository;
    private final TaskDependencyRepository taskDependencyRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;

    public ProjectService(ProjectRepository projects, CurrentUserService currentUserService,
                          TaskRepository taskRepository, TeamMemberRepository teamMemberRepository,
                          ProjectRiskRepository projectRiskRepository, TaskDependencyRepository taskDependencyRepository,
                          TaskAssignmentRepository taskAssignmentRepository) {
        this.projects = projects;
        this.currentUserService = currentUserService;
        this.taskRepository = taskRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.projectRiskRepository = projectRiskRepository;
        this.taskDependencyRepository = taskDependencyRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
    }

    public ProjectResponse createProject(CreateProjectRequest r) {
        Long userId = currentUserService.getCurrentUserId();
        Project p = new Project();
        p.setOwnerId(userId);
        apply(p, r.name(), r.description(), r.startDate(), r.endDate(), r.budget(), r.methodology(), r.status() == null ? ProjectStatus.DRAFT : r.status());
        return toResponse(projects.save(p));
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        Long userId = currentUserService.getCurrentUserId();
        return toResponse(owned(userId, id));
    }

    @Transactional(readOnly = true)
    public List<ProjectSummaryResponse> listProjectsForUser() {
        Long userId = currentUserService.getCurrentUserId();
        return projects.findAllByOwnerIdOrderByUpdatedAtDesc(userId).stream()
                .map(p -> new ProjectSummaryResponse(p.getId(), p.getName(), p.getStatus(), p.getStartDate(), p.getEndDate()))
                .toList();
    }

    public ProjectResponse updateProject(Long id, UpdateProjectRequest r) {
        Long userId = currentUserService.getCurrentUserId();
        Project p = owned(userId, id);
        apply(p, r.name(), r.description(), r.startDate(), r.endDate(), r.budget(), r.methodology(), r.status());
        return toResponse(p);
    }

    public void deleteProject(Long id) {
        Long userId = currentUserService.getCurrentUserId();
        Project p = owned(userId, id);
        
        // Cascading deletes
        taskDependencyRepository.deleteAllByProjectId(id);
        taskAssignmentRepository.deleteByProjectId(id);
        taskRepository.deleteByProjectId(id);
        teamMemberRepository.deleteByProjectId(id);
        projectRiskRepository.deleteByProjectId(id);
        
        projects.delete(p);
    }

    private Project owned(Long userId, Long id) {
        Project p = projects.findById(id).orElseThrow(() -> new NotFoundException("Project not found"));
        if (!p.getOwnerId().equals(userId)) {
            throw new ForbiddenException("You do not have access to this project");
        }
        return p;
    }

    private void apply(Project p, String n, String d, java.time.LocalDate s, java.time.LocalDate e, java.math.BigDecimal b, String m, ProjectStatus st) {
        if (s != null && e != null && e.isBefore(s)) {
            throw new IllegalArgumentException("End date must be on or after start date");
        }
        p.setName(n);
        p.setDescription(d);
        p.setStartDate(s);
        p.setEndDate(e);
        p.setBudget(b);
        p.setMethodology(m);
        p.setStatus(st);
    }

    private ProjectResponse toResponse(Project p) {
        return new ProjectResponse(p.getId(), p.getOwnerId(), p.getName(), p.getDescription(), p.getStartDate(), p.getEndDate(), p.getBudget(), p.getMethodology(), p.getStatus(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
