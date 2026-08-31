package com.example.aiprojectmanager.simulation.service;

import com.example.aiprojectmanager.common.NotFoundException;
import com.example.aiprojectmanager.project.domain.Project;
import com.example.aiprojectmanager.project.repository.ProjectRepository;
import com.example.aiprojectmanager.scheduling.domain.TaskDependency;
import com.example.aiprojectmanager.scheduling.dto.GanttTaskItem;
import com.example.aiprojectmanager.scheduling.repository.TaskDependencyRepository;
import com.example.aiprojectmanager.scheduling.service.SchedulingService;
import com.example.aiprojectmanager.simulation.dto.SimulationRequest;
import com.example.aiprojectmanager.simulation.dto.SimulationResultDto;
import com.example.aiprojectmanager.simulation.dto.TaskSimulationOverride;
import com.example.aiprojectmanager.task.domain.Task;
import com.example.aiprojectmanager.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SimulationService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final TaskDependencyRepository dependencyRepository;
    private final SchedulingService schedulingService;

    public SimulationResultDto simulateScenario(Long projectId, Long ownerId, SimulationRequest request) {
        Project project = projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new NotFoundException("Project not found or access denied"));

        List<Task> originalTasks = taskRepository.findAllByProjectIdOrderByDueDateAsc(projectId);
        List<TaskDependency> dependencies = dependencyRepository.findAllByProjectId(projectId);

        if (originalTasks.isEmpty()) {
            throw new IllegalArgumentException("Cannot simulate a project with no tasks.");
        }

        // 1. Calculate Baseline CPM
        List<GanttTaskItem> baselineGantt = schedulingService.calculateTaskDates(projectId);
        int baselineDurationDays = baselineGantt.stream()
                .mapToInt(g -> g.durationDays() != null ? g.durationDays() : 1)
                .max().orElse(30);

        List<Long> baselineCriticalPath = baselineGantt.stream()
                .filter(GanttTaskItem::isCritical)
                .map(GanttTaskItem::id)
                .toList();

        int baselineCriticalLength = baselineGantt.stream()
                .filter(GanttTaskItem::isCritical)
                .mapToInt(GanttTaskItem::durationDays)
                .sum();

        LocalDate projectStart = project.getStartDate() != null ? project.getStartDate() : LocalDate.now();
        LocalDate baselineFinish = projectStart.plusDays(baselineCriticalLength > 0 ? baselineCriticalLength : baselineDurationDays);
        BigDecimal baselineCost = project.getBudget() != null ? project.getBudget() : BigDecimal.valueOf(100000);

        // 2. Build In-Memory Simulated Task Graph
        Map<Long, TaskSimulationOverride> overrideMap = request.getTaskOverrides() != null
                ? request.getTaskOverrides().stream().collect(Collectors.toMap(TaskSimulationOverride::getTaskId, o -> o, (a, b) -> b))
                : Collections.emptyMap();

        int devDelta = request.getDeveloperDelta() != null ? request.getDeveloperDelta() : 0;
        double prodMultiplier = request.getProductivityMultiplier() != null ? request.getProductivityMultiplier().doubleValue() : 1.0;

        // Brooks' Law modeling: each added developer adds 18% speedup up to a cap of diminishing returns
        double speedupFactor = 1.0;
        if (devDelta > 0) {
            double effectiveBoost = Math.min(devDelta * 0.20, 0.65); // max 65% speedup
            speedupFactor = Math.max(0.45, 1.0 - (effectiveBoost * prodMultiplier));
        } else if (devDelta < 0) {
            speedupFactor = 1.0 + (Math.abs(devDelta) * 0.28);
        }

        List<Task> simulatedTasks = new ArrayList<>();
        Set<Long> excludedTaskIds = new HashSet<>();

        for (Task t : originalTasks) {
            TaskSimulationOverride ov = overrideMap.get(t.getId());
            if (ov != null && Boolean.TRUE.equals(ov.getExcludeFromScope())) {
                excludedTaskIds.add(t.getId());
                continue; // Cut feature / scope removed
            }

            Task simTask = new Task();
            simTask.setId(t.getId());
            simTask.setProjectId(t.getProjectId());
            simTask.setTitle(t.getTitle());
            simTask.setStatus(t.getStatus());
            simTask.setPriority(t.getPriority());
            simTask.setProgressPercentage(t.getProgressPercentage());

            int baseDuration = t.getDurationDays() != null && t.getDurationDays() > 0 ? t.getDurationDays() : 1;
            int newDuration = baseDuration;

            if (ov != null && ov.getNewDurationDays() != null && ov.getNewDurationDays() > 0) {
                newDuration = ov.getNewDurationDays();
            } else if (ov != null && ov.getDurationDeltaDays() != null) {
                newDuration = Math.max(1, baseDuration + ov.getDurationDeltaDays());
            } else {
                newDuration = Math.max(1, (int) Math.round(baseDuration * speedupFactor));
            }

            simTask.setDurationDays(newDuration);
            simulatedTasks.add(simTask);
        }

        // Filter dependencies for remaining tasks
        List<TaskDependency> simulatedDeps = dependencies.stream()
                .filter(d -> !excludedTaskIds.contains(d.getPredecessorTaskId()) && !excludedTaskIds.contains(d.getSuccessorTaskId()))
                .toList();

        // 3. Execute CPM on Simulated Graph
        List<Task> ordered;
        try {
            ordered = schedulingService.topologicalSort(simulatedTasks, simulatedDeps);
        } catch (Exception e) {
            ordered = new ArrayList<>(simulatedTasks);
        }

        Map<Long, Integer> es = new HashMap<>();
        Map<Long, Integer> ef = new HashMap<>();
        Map<Long, List<TaskDependency>> predsOf = simulatedDeps.stream()
                .collect(Collectors.groupingBy(TaskDependency::getSuccessorTaskId));

        for (Task t : ordered) {
            List<TaskDependency> inEdges = predsOf.getOrDefault(t.getId(), List.of());
            int startDay = 0;
            for (TaskDependency dep : inEdges) {
                int predEf = ef.getOrDefault(dep.getPredecessorTaskId(), 0);
                startDay = Math.max(startDay, predEf + (dep.getLagDays() != null ? dep.getLagDays() : 0));
            }
            int duration = t.getDurationDays() != null ? t.getDurationDays() : 1;
            es.put(t.getId(), startDay);
            ef.put(t.getId(), startDay + duration);
        }

        int simProjectFinishDay = ef.values().stream().max(Integer::compareTo).orElse(1);

        Map<Long, Integer> ls = new HashMap<>();
        Map<Long, Integer> lf = new HashMap<>();
        Map<Long, List<TaskDependency>> succsOf = simulatedDeps.stream()
                .collect(Collectors.groupingBy(TaskDependency::getPredecessorTaskId));

        List<Task> reversed = new ArrayList<>(ordered);
        Collections.reverse(reversed);

        for (Task t : reversed) {
            List<TaskDependency> outEdges = succsOf.getOrDefault(t.getId(), List.of());
            int finishDay = simProjectFinishDay;
            for (TaskDependency dep : outEdges) {
                int succLs = ls.getOrDefault(dep.getSuccessorTaskId(), simProjectFinishDay);
                finishDay = Math.min(finishDay, succLs - (dep.getLagDays() != null ? dep.getLagDays() : 0));
            }
            int duration = t.getDurationDays() != null ? t.getDurationDays() : 1;
            lf.put(t.getId(), finishDay);
            ls.put(t.getId(), finishDay - duration);
        }

        List<GanttTaskItem> simulatedGanttItems = new ArrayList<>();
        List<Long> simulatedCriticalPath = new ArrayList<>();

        for (Task t : simulatedTasks) {
            int taskEs = es.getOrDefault(t.getId(), 0);
            int taskEf = ef.getOrDefault(t.getId(), t.getDurationDays());
            int taskLs = ls.getOrDefault(t.getId(), taskEs);
            int slack = Math.max(0, taskLs - taskEs);
            boolean isCritical = (slack == 0);

            if (isCritical) {
                simulatedCriticalPath.add(t.getId());
            }

            LocalDate start = projectStart.plusDays(taskEs);
            LocalDate end = projectStart.plusDays(taskEf);

            List<Long> predIds = predsOf.getOrDefault(t.getId(), List.of()).stream()
                    .map(TaskDependency::getPredecessorTaskId)
                    .toList();

            simulatedGanttItems.add(new GanttTaskItem(
                    t.getId(),
                    t.getTitle(),
                    start,
                    end,
                    t.getDurationDays(),
                    t.getProgressPercentage(),
                    predIds,
                    isCritical,
                    t.getStatus(),
                    t.getPriority()
            ));
        }

        LocalDate simFinish = projectStart.plusDays(simProjectFinishDay);
        int durationDelta = simProjectFinishDay - (baselineCriticalLength > 0 ? baselineCriticalLength : baselineDurationDays);

        // 4. Calculate Financial Impact
        BigDecimal rate = request.getDeveloperHourlyRate() != null ? request.getDeveloperHourlyRate() : BigDecimal.valueOf(500);
        BigDecimal additionalDevCost = BigDecimal.valueOf(devDelta)
                .multiply(rate)
                .multiply(BigDecimal.valueOf(8)) // 8 hrs/day
                .multiply(BigDecimal.valueOf(simProjectFinishDay));

        BigDecimal simulatedCost = baselineCost.add(additionalDevCost).max(BigDecimal.ZERO);
        BigDecimal costDelta = simulatedCost.subtract(baselineCost);

        // 5. Prescriptive Advice & Feasibility
        String feasibility;
        List<String> recommendations = new ArrayList<>();

        if (devDelta > 4) {
            feasibility = "DIMINISHING_RETURNS";
            recommendations.add("Adding more than 4 developers incurs significant communication and coordination overhead (Brooks' Law).");
        } else if (durationDelta < -7 && costDelta.compareTo(baselineCost.multiply(BigDecimal.valueOf(0.35))) <= 0) {
            feasibility = "OPTIMAL";
            recommendations.add(String.format("Recommended Strategy: Accelerates delivery by %d days for an investment of ₹%s.", Math.abs(durationDelta), costDelta.setScale(0, RoundingMode.HALF_UP)));
        } else if (durationDelta > 10) {
            feasibility = "HIGH_RISK";
            recommendations.add(String.format("Critical warning: This scenario increases project duration by %d days, delaying delivery to %s.", durationDelta, simFinish));
        } else {
            feasibility = "FEASIBLE";
        }

        if (!excludedTaskIds.isEmpty()) {
            recommendations.add(String.format("Trimming %d optional task(s) removed dependencies from the critical path, saving scheduled lead time.", excludedTaskIds.size()));
        }

        if (devDelta < 0) {
            recommendations.add(String.format("Reducing team size lowers headcount cost by ₹%s but pushes delivery out by %d days.", costDelta.abs().setScale(0, RoundingMode.HALF_UP), Math.abs(durationDelta)));
        }

        return SimulationResultDto.builder()
                .projectId(projectId)
                .baselineDurationDays(baselineCriticalLength > 0 ? baselineCriticalLength : baselineDurationDays)
                .baselineFinishDate(baselineFinish)
                .baselineEstimatedCost(baselineCost)
                .baselineCriticalPathLength(baselineCriticalPath.size())
                .simulatedDurationDays(simProjectFinishDay)
                .simulatedFinishDate(simFinish)
                .simulatedEstimatedCost(simulatedCost)
                .simulatedCriticalPathLength(simulatedCriticalPath.size())
                .durationDeltaDays(durationDelta)
                .costDelta(costDelta)
                .feasibilityAssessment(feasibility)
                .prescriptiveRecommendations(recommendations)
                .simulatedTasks(simulatedGanttItems)
                .simulatedCriticalPath(simulatedCriticalPath)
                .build();
    }
}
