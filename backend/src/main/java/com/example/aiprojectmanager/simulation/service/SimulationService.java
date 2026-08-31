package com.example.aiprojectmanager.simulation.service;

import com.example.aiprojectmanager.ai.domain.DomainBenchmarkProfile;
import com.example.aiprojectmanager.ai.service.IndustryBenchmarkCorpus;
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
    private final IndustryBenchmarkCorpus benchmarkCorpus;

    public SimulationResultDto simulateScenario(Long projectId, Long ownerId, SimulationRequest request) {
        Project project = projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new NotFoundException("Project not found or access denied"));

        List<Task> originalTasks = taskRepository.findAllByProjectIdOrderByDueDateAsc(projectId);
        List<TaskDependency> dependencies = dependencyRepository.findAllByProjectId(projectId);

        if (originalTasks.isEmpty()) {
            throw new IllegalArgumentException("Cannot simulate a project with no tasks.");
        }

        DomainBenchmarkProfile domainProfile = benchmarkCorpus.getProfileForProject(
                project.getName(), project.getDescription(), project.getMethodology()
        );

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

        // Calibrated Brooks' Law & COCOMO II Scaling
        double speedupFactor = 1.0;
        if (devDelta > 0) {
            // Non-linear scaling with communication overhead penalty
            double rawBoost = devDelta * 0.22 * prodMultiplier;
            double communicationPenalty = (devDelta * (devDelta - 1)) * 0.015;
            double effectiveBoost = Math.max(0.05, Math.min(rawBoost - communicationPenalty, 0.70));
            speedupFactor = Math.max(0.40, 1.0 - effectiveBoost);
        } else if (devDelta < 0) {
            speedupFactor = 1.0 + (Math.abs(devDelta) * 0.30);
        }

        List<Task> simulatedTasks = new ArrayList<>();
        Set<Long> excludedTaskIds = new HashSet<>();

        for (Task t : originalTasks) {
            TaskSimulationOverride ov = overrideMap.get(t.getId());
            if (ov != null && Boolean.TRUE.equals(ov.getExcludeFromScope())) {
                excludedTaskIds.add(t.getId());
                continue;
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

            simulatedGanttItems.add(new GanttTaskItem(
                    t.getId(),
                    t.getTitle(),
                    projectStart.plusDays(taskEs),
                    projectStart.plusDays(taskEf),
                    t.getDurationDays(),
                    t.getProgressPercentage() != null ? t.getProgressPercentage() : 0,
                    simulatedDeps.stream().filter(d -> d.getSuccessorTaskId().equals(t.getId())).map(TaskDependency::getPredecessorTaskId).toList(),
                    isCritical,
                    t.getStatus(),
                    t.getPriority()
            ));
        }

        int simulatedCriticalLength = simulatedGanttItems.stream()
                .filter(GanttTaskItem::isCritical)
                .mapToInt(GanttTaskItem::durationDays)
                .sum();

        LocalDate simulatedFinish = projectStart.plusDays(simulatedCriticalLength > 0 ? simulatedCriticalLength : simProjectFinishDay);
        int durationDeltaDays = (int) java.time.temporal.ChronoUnit.DAYS.between(baselineFinish, simulatedFinish);

        // 4. Financial & Labor Cost Impact Estimation
        BigDecimal hourlyRate = request.getDeveloperHourlyRate() != null && request.getDeveloperHourlyRate().compareTo(BigDecimal.ZERO) > 0
                ? request.getDeveloperHourlyRate()
                : BigDecimal.valueOf(650);

        int simDurationWeeks = Math.max(1, (simulatedCriticalLength > 0 ? simulatedCriticalLength : simProjectFinishDay) / 5);
        BigDecimal devCostDelta = BigDecimal.valueOf(devDelta)
                .multiply(hourlyRate)
                .multiply(BigDecimal.valueOf(40))
                .multiply(BigDecimal.valueOf(simDurationWeeks));

        BigDecimal simulatedCost = baselineCost.add(devCostDelta).setScale(2, RoundingMode.HALF_UP);
        if (simulatedCost.compareTo(BigDecimal.ZERO) < 0) simulatedCost = BigDecimal.ZERO;

        BigDecimal costDelta = simulatedCost.subtract(baselineCost);

        // 5. Prescriptive AI Recommendations & Feasibility
        String feasibility;
        List<String> recs = new ArrayList<>();

        if (durationDeltaDays < 0 && costDelta.compareTo(BigDecimal.ZERO) <= 0) {
            feasibility = "OPTIMAL";
            recs.add(String.format("🚀 High Efficiency: Schedule compressed by %d days while staying within budget.", Math.abs(durationDeltaDays)));
        } else if (durationDeltaDays < 0) {
            feasibility = "FEASIBLE";
            recs.add(String.format("⏱️ Timeline Compressed: Fast-tracks delivery by %d days with an additional investment of ₹%s.", Math.abs(durationDeltaDays), costDelta.toPlainString()));
        } else if (durationDeltaDays > 0) {
            feasibility = "HIGH_RISK";
            recs.add(String.format("⚠️ Delay Warning: Scenario extends critical timeline by +%d days. Review critical path blockers.", durationDeltaDays));
        } else {
            feasibility = "FEASIBLE";
            recs.add("⚖️ Schedule timeline unchanged. Cost adjustments reflect resource reassignment.");
        }

        if (devDelta > 4) {
            feasibility = "DIMINISHING_RETURNS";
            recs.add("📉 Brooks' Law Warning: Adding >4 engineers simultaneously introduces steep communication & onboarding overhead.");
        }

        if (!excludedTaskIds.isEmpty()) {
            recs.add(String.format("✂️ Scope De-scoped: %d task(s) removed from critical path, reducing delivery variance.", excludedTaskIds.size()));
        }

        recs.add("🎯 Domain Benchmark (" + domainProfile.getDomainName() + "): Recommended team capacity aligns with historical delivery standards.");

        return SimulationResultDto.builder()
                .projectId(projectId)
                .baselineDurationDays(baselineCriticalLength > 0 ? baselineCriticalLength : baselineDurationDays)
                .baselineFinishDate(baselineFinish)
                .baselineEstimatedCost(baselineCost)
                .baselineCriticalPathLength(baselineCriticalLength)
                .simulatedDurationDays(simulatedCriticalLength > 0 ? simulatedCriticalLength : simProjectFinishDay)
                .simulatedFinishDate(simulatedFinish)
                .simulatedEstimatedCost(simulatedCost)
                .simulatedCriticalPathLength(simulatedCriticalLength)
                .durationDeltaDays(durationDeltaDays)
                .costDelta(costDelta)
                .feasibilityAssessment(feasibility)
                .prescriptiveRecommendations(recs)
                .simulatedTasks(simulatedGanttItems)
                .simulatedCriticalPath(simulatedCriticalPath)
                .build();
    }
}
