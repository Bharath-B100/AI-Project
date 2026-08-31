package com.example.aiprojectmanager.assignment.service;

import com.example.aiprojectmanager.assignment.domain.TaskAssignment;
import com.example.aiprojectmanager.assignment.dto.LevelingRecommendationDto;
import com.example.aiprojectmanager.assignment.dto.LevelingReportDto;
import com.example.aiprojectmanager.assignment.repository.TaskAssignmentRepository;
import com.example.aiprojectmanager.common.NotFoundException;
import com.example.aiprojectmanager.project.domain.Project;
import com.example.aiprojectmanager.project.repository.ProjectRepository;
import com.example.aiprojectmanager.scheduling.dto.GanttTaskItem;
import com.example.aiprojectmanager.scheduling.service.SchedulingService;
import com.example.aiprojectmanager.task.domain.Task;
import com.example.aiprojectmanager.task.domain.TaskStatus;
import com.example.aiprojectmanager.task.repository.TaskRepository;
import com.example.aiprojectmanager.team.domain.TeamMember;
import com.example.aiprojectmanager.team.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceLevelingService {

    private final ProjectRepository projectRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final TaskRepository taskRepository;
    private final SchedulingService schedulingService;

    @Transactional(readOnly = true)
    public LevelingReportDto calculateLevelingRecommendations(Long projectId, Long ownerId) {
        Project project = projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new NotFoundException("Project not found or access denied"));

        List<TeamMember> members = teamMemberRepository.findByProjectId(projectId);
        List<TaskAssignment> assignments = taskAssignmentRepository.findByProjectId(projectId);
        List<GanttTaskItem> ganttTasks = Collections.emptyList();
        try {
            ganttTasks = schedulingService.calculateTaskDates(projectId);
        } catch (Exception ignored) {}

        Set<Long> criticalTaskIds = ganttTasks.stream()
                .filter(GanttTaskItem::isCritical)
                .map(GanttTaskItem::id)
                .collect(Collectors.toSet());

        // 1. Calculate each member's workload percentage
        Map<Long, BigDecimal> memberPlannedHours = new HashMap<>();
        Map<Long, List<TaskAssignment>> memberAssignments = new HashMap<>();

        for (TeamMember m : members) {
            memberPlannedHours.put(m.getId(), BigDecimal.ZERO);
            memberAssignments.put(m.getId(), new ArrayList<>());
        }

        for (TaskAssignment ta : assignments) {
            Long memId = ta.getTeamMember().getId();
            BigDecimal hours = ta.getPlannedHours() != null ? ta.getPlannedHours() : BigDecimal.valueOf(10);
            memberPlannedHours.put(memId, memberPlannedHours.getOrDefault(memId, BigDecimal.ZERO).add(hours));
            memberAssignments.computeIfAbsent(memId, k -> new ArrayList<>()).add(ta);
        }

        Map<Long, BigDecimal> memberWorkloadPct = new HashMap<>();
        List<TeamMember> overloadedMembers = new ArrayList<>();
        List<TeamMember> availableMembers = new ArrayList<>();

        for (TeamMember m : members) {
            BigDecimal weeklyCapacity = m.getAvailabilityHoursPerWeek() != null && m.getAvailabilityHoursPerWeek().compareTo(BigDecimal.ZERO) > 0
                    ? m.getAvailabilityHoursPerWeek() : BigDecimal.valueOf(40.0);
            
            // Assume 4-week active sprint horizon for capacity
            BigDecimal totalHorizonCapacity = weeklyCapacity.multiply(BigDecimal.valueOf(4));
            BigDecimal planned = memberPlannedHours.getOrDefault(m.getId(), BigDecimal.ZERO);
            
            BigDecimal pct = planned.divide(totalHorizonCapacity, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
            memberWorkloadPct.put(m.getId(), pct);

            if (pct.compareTo(BigDecimal.valueOf(100.0)) > 0) {
                overloadedMembers.add(m);
            } else if (pct.compareTo(BigDecimal.valueOf(70.0)) < 0) {
                availableMembers.add(m);
            }
        }

        // 2. Generate prescriptive leveling recommendations
        List<LevelingRecommendationDto> recommendations = new ArrayList<>();

        // Sort available members by lowest load
        availableMembers.sort(Comparator.comparing(m -> memberWorkloadPct.getOrDefault(m.getId(), BigDecimal.ZERO)));

        for (TeamMember overloaded : overloadedMembers) {
            List<TaskAssignment> overAssigned = memberAssignments.getOrDefault(overloaded.getId(), List.of());

            // Prioritize reallocating non-critical tasks first, or tasks in TODO status
            List<TaskAssignment> candidateAssignments = overAssigned.stream()
                    .filter(a -> a.getTask().getStatus() != TaskStatus.DONE)
                    .sorted(Comparator.comparing(a -> criticalTaskIds.contains(a.getTask().getId()))) // false (non-critical) first
                    .toList();

            for (TaskAssignment candidate : candidateAssignments) {
                if (availableMembers.isEmpty()) break;

                // Pick the best available candidate (matching role if possible)
                TeamMember target = availableMembers.stream()
                        .filter(m -> m.getRole() != null && overloaded.getRole() != null && m.getRole().equalsIgnoreCase(overloaded.getRole()))
                        .findFirst()
                        .orElse(availableMembers.get(0));

                BigDecimal taskHours = candidate.getPlannedHours() != null ? candidate.getPlannedHours() : BigDecimal.valueOf(10);
                boolean isNonCritical = !criticalTaskIds.contains(candidate.getTask().getId());

                String rationale = String.format(
                        "Reassign '%s' from %s (%s%% capacity) to %s (%s%% capacity) to eliminate bottleneck%s.",
                        candidate.getTask().getTitle(),
                        overloaded.getName(),
                        memberWorkloadPct.get(overloaded.getId()),
                        target.getName(),
                        memberWorkloadPct.get(target.getId()),
                        isNonCritical ? " without affecting the project critical path" : ""
                );

                recommendations.add(LevelingRecommendationDto.builder()
                        .taskId(candidate.getTask().getId())
                        .taskTitle(candidate.getTask().getTitle())
                        .taskDurationDays(candidate.getTask().getDurationDays())
                        .plannedHours(taskHours)
                        .sourceMemberId(overloaded.getId())
                        .sourceMemberName(overloaded.getName())
                        .sourceCurrentWorkloadPct(memberWorkloadPct.get(overloaded.getId()))
                        .targetMemberId(target.getId())
                        .targetMemberName(target.getName())
                        .targetCurrentWorkloadPct(memberWorkloadPct.get(target.getId()))
                        .rationale(rationale)
                        .build());

                // Simulated update to avoid over-assigning target in suggestions
                BigDecimal targetCap = target.getAvailabilityHoursPerWeek() != null ? target.getAvailabilityHoursPerWeek().multiply(BigDecimal.valueOf(4)) : BigDecimal.valueOf(160);
                BigDecimal newTargetLoad = memberPlannedHours.get(target.getId()).add(taskHours)
                        .divide(targetCap, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
                memberWorkloadPct.put(target.getId(), newTargetLoad);

                if (newTargetLoad.compareTo(BigDecimal.valueOf(80.0)) >= 0) {
                    availableMembers.remove(target);
                }
                break; // 1-2 key suggestions per overloaded member
            }
        }

        String portfolioStatus = overloadedMembers.isEmpty() ? "OPTIMAL" : (availableMembers.isEmpty() ? "OVERLOADED" : "UNBALANCED");

        return LevelingReportDto.builder()
                .projectId(projectId)
                .totalTeamMembers(members.size())
                .overloadedCount(overloadedMembers.size())
                .availableCount(availableMembers.size())
                .portfolioWorkloadStatus(portfolioStatus)
                .recommendations(recommendations)
                .build();
    }

    @Transactional
    public Map<String, Object> applyLevelingRecommendations(Long projectId, Long ownerId) {
        LevelingReportDto report = calculateLevelingRecommendations(projectId, ownerId);
        int appliedCount = 0;

        for (LevelingRecommendationDto rec : report.getRecommendations()) {
            Optional<TaskAssignment> existing = taskAssignmentRepository.findByTaskIdAndTeamMemberId(rec.getTaskId(), rec.getSourceMemberId());
            TeamMember targetMember = teamMemberRepository.findById(rec.getTargetMemberId()).orElse(null);

            if (existing.isPresent() && targetMember != null) {
                TaskAssignment assignment = existing.get();
                assignment.setTeamMember(targetMember);
                taskAssignmentRepository.save(assignment);
                appliedCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("appliedReallocations", appliedCount);
        result.put("message", appliedCount > 0
                ? String.format("Successfully reallocated %d task(s) to balance team workload.", appliedCount)
                : "Workload is already balanced. No reallocations needed.");

        return result;
    }
}
