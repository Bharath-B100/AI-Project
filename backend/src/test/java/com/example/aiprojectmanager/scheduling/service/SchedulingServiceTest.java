package com.example.aiprojectmanager.scheduling.service;

import com.example.aiprojectmanager.scheduling.domain.TaskDependency;
import com.example.aiprojectmanager.scheduling.dto.*;
import com.example.aiprojectmanager.scheduling.repository.TaskDependencyRepository;
import com.example.aiprojectmanager.task.domain.DependencyType;
import com.example.aiprojectmanager.task.domain.Task;
import com.example.aiprojectmanager.task.domain.TaskPriority;
import com.example.aiprojectmanager.task.domain.TaskStatus;
import com.example.aiprojectmanager.task.repository.TaskRepository;
import com.example.aiprojectmanager.project.domain.Project;
import com.example.aiprojectmanager.project.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the CPM scheduling engine.
 * Uses Mockito stubs — no Spring context, no database required.
 */
@ExtendWith(MockitoExtension.class)
class SchedulingServiceTest {

    @Mock private TaskRepository taskRepo;
    @Mock private TaskDependencyRepository depRepo;
    @Mock private ProjectRepository projectRepo;

    @InjectMocks
    private SchedulingService service;

    private static final Long PROJECT_ID = 1L;
    private static final LocalDate PROJECT_START = LocalDate.of(2025, 1, 1);

    // ── Helpers ────────────────────────────────────────────────────────────

    private Task task(Long id, String title, int durationDays) {
        Task t = new Task();
        t.setId(id);
        t.setProjectId(PROJECT_ID);
        t.setTitle(title);
        t.setDurationDays(durationDays);
        t.setStatus(TaskStatus.TODO);
        t.setPriority(TaskPriority.MEDIUM);
        t.setProgressPercentage(0);
        return t;
    }

    private TaskDependency dep(Long id, Long pred, Long succ) {
        return dep(id, pred, succ, DependencyType.FINISH_TO_START, 0);
    }

    private TaskDependency dep(Long id, Long pred, Long succ, DependencyType type, int lag) {
        TaskDependency d = new TaskDependency();
        d.setId(id);
        d.setProjectId(PROJECT_ID);
        d.setPredecessorTaskId(pred);
        d.setSuccessorTaskId(succ);
        d.setDependencyType(type);
        d.setLagDays(lag);
        return d;
    }

    private Project project() {
        Project p = new Project();
        p.setId(PROJECT_ID);
        p.setName("Test Project");
        p.setStartDate(PROJECT_START);
        return p;
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Linear chain A(3) → B(2) → C(4): correct ES/EF and critical path")
    void testLinearChain() {
        Task a = task(10L, "A", 3);
        Task b = task(20L, "B", 2);
        Task c = task(30L, "C", 4);

        List<Task> tasks = List.of(a, b, c);
        List<TaskDependency> deps = List.of(dep(1L, 10L, 20L), dep(2L, 20L, 30L));

        when(projectRepo.findById(PROJECT_ID)).thenReturn(Optional.of(project()));
        when(taskRepo.findAllByProjectIdOrderByDueDateAsc(PROJECT_ID)).thenReturn(tasks);
        when(depRepo.findAllByProjectId(PROJECT_ID)).thenReturn(deps);

        List<GanttTaskItem> result = service.calculateTaskDates(PROJECT_ID);

        assertThat(result).hasSize(3);

        GanttTaskItem ga = result.stream().filter(t -> t.id().equals(10L)).findFirst().orElseThrow();
        GanttTaskItem gb = result.stream().filter(t -> t.id().equals(20L)).findFirst().orElseThrow();
        GanttTaskItem gc = result.stream().filter(t -> t.id().equals(30L)).findFirst().orElseThrow();

        // A: starts on day 0 (project start), ends on day 3
        assertThat(ga.scheduledStart()).isEqualTo(PROJECT_START);
        assertThat(ga.scheduledEnd()).isEqualTo(PROJECT_START.plusDays(3));

        // B: starts after A finishes (day 3), ends day 5
        assertThat(gb.scheduledStart()).isEqualTo(PROJECT_START.plusDays(3));
        assertThat(gb.scheduledEnd()).isEqualTo(PROJECT_START.plusDays(5));

        // C: starts after B finishes (day 5), ends day 9
        assertThat(gc.scheduledStart()).isEqualTo(PROJECT_START.plusDays(5));
        assertThat(gc.scheduledEnd()).isEqualTo(PROJECT_START.plusDays(9));

        // All tasks are on the critical path (linear chain has no slack)
        assertThat(ga.isCritical()).isTrue();
        assertThat(gb.isCritical()).isTrue();
        assertThat(gc.isCritical()).isTrue();
    }

    @Test
    @DisplayName("Multiple predecessors: D starts after max(B.EF, C.EF)")
    void testMultiplePredecessors() {
        // A(1) → B(5)
        //      → C(2) → D(1)
        // D also depends on B → D starts after max(B.EF=6, C.EF=3) = day 6
        Task a = task(1L, "A", 1);
        Task b = task(2L, "B", 5);
        Task c = task(3L, "C", 2);
        Task d = task(4L, "D", 1);

        List<Task> tasks = List.of(a, b, c, d);
        List<TaskDependency> deps = List.of(
                dep(1L, 1L, 2L),  // A→B
                dep(2L, 1L, 3L),  // A→C
                dep(3L, 2L, 4L),  // B→D
                dep(4L, 3L, 4L)   // C→D
        );

        when(projectRepo.findById(PROJECT_ID)).thenReturn(Optional.of(project()));
        when(taskRepo.findAllByProjectIdOrderByDueDateAsc(PROJECT_ID)).thenReturn(tasks);
        when(depRepo.findAllByProjectId(PROJECT_ID)).thenReturn(deps);

        List<GanttTaskItem> result = service.calculateTaskDates(PROJECT_ID);

        GanttTaskItem gd = result.stream().filter(t -> t.id().equals(4L)).findFirst().orElseThrow();
        GanttTaskItem gb = result.stream().filter(t -> t.id().equals(2L)).findFirst().orElseThrow();
        GanttTaskItem gc = result.stream().filter(t -> t.id().equals(3L)).findFirst().orElseThrow();

        // B finishes on day 6 (starts day 1, duration 5)
        assertThat(gb.scheduledEnd()).isEqualTo(PROJECT_START.plusDays(6));
        // C finishes on day 3 (starts day 1, duration 2)
        assertThat(gc.scheduledEnd()).isEqualTo(PROJECT_START.plusDays(3));
        // D starts after max(B.EF, C.EF) = day 6
        assertThat(gd.scheduledStart()).isEqualTo(PROJECT_START.plusDays(6));
        assertThat(gd.scheduledEnd()).isEqualTo(PROJECT_START.plusDays(7));

        // Critical path: A→B→D (total 7 days: A=1, B=5, D=1)
        assertThat(gd.isCritical()).isTrue();
        assertThat(gb.isCritical()).isTrue();
    }

    @Test
    @DisplayName("Circular dependency A→B→A is detected and throws IllegalArgumentException")
    void testCircularDependencyDetected() {
        Task a = task(1L, "A", 2);
        Task b = task(2L, "B", 3);

        List<Task> tasks = List.of(a, b);
        List<TaskDependency> deps = List.of(
                dep(1L, 1L, 2L),  // A→B
                dep(2L, 2L, 1L)   // B→A  (creates cycle)
        );

        assertThatThrownBy(() -> service.topologicalSort(tasks, deps))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Circular dependency");
    }

    @Test
    @DisplayName("Self-dependency: topological sort detects task depending on itself")
    void testSelfDependencyDetected() {
        Task a = task(1L, "A", 1);
        List<Task> tasks = List.of(a);
        List<TaskDependency> deps = List.of(dep(1L, 1L, 1L)); // A→A

        assertThatThrownBy(() -> service.topologicalSort(tasks, deps))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Circular dependency");
    }

    @Test
    @DisplayName("Critical path calculation: parallel branches, only longest is critical")
    void testCriticalPath() {
        // START → A(5) → END
        //       → B(2) → END
        // Critical path: A (duration 5 > 2)
        Task a = task(1L, "A", 5);
        Task b = task(2L, "B", 2);

        List<Task> tasks = List.of(a, b);
        List<TaskDependency> deps = Collections.emptyList(); // independent tasks

        when(projectRepo.findById(PROJECT_ID)).thenReturn(Optional.of(project()));
        when(taskRepo.findAllByProjectIdOrderByDueDateAsc(PROJECT_ID)).thenReturn(tasks);
        when(depRepo.findAllByProjectId(PROJECT_ID)).thenReturn(deps);

        CriticalPathResponse cp = service.getCriticalPath(PROJECT_ID);

        // Only task A (duration 5) is critical; B has slack 3
        assertThat(cp.tasks()).hasSize(1);
        assertThat(cp.tasks().get(0).id()).isEqualTo(1L);
        assertThat(cp.totalDurationDays()).isEqualTo(5);
    }

    @Test
    @DisplayName("Lag days: B starts 2 days after A finishes (FS + lag=2)")
    void testLagDays() {
        Task a = task(1L, "A", 3);
        Task b = task(2L, "B", 2);

        List<Task> tasks = List.of(a, b);
        List<TaskDependency> deps = List.of(
                dep(1L, 1L, 2L, DependencyType.FINISH_TO_START, 2)
        );

        when(projectRepo.findById(PROJECT_ID)).thenReturn(Optional.of(project()));
        when(taskRepo.findAllByProjectIdOrderByDueDateAsc(PROJECT_ID)).thenReturn(tasks);
        when(depRepo.findAllByProjectId(PROJECT_ID)).thenReturn(deps);

        List<GanttTaskItem> result = service.calculateTaskDates(PROJECT_ID);

        GanttTaskItem gb = result.stream().filter(t -> t.id().equals(2L)).findFirst().orElseThrow();
        // A finishes day 3, lag=2 → B starts day 5
        assertThat(gb.scheduledStart()).isEqualTo(PROJECT_START.plusDays(5));
        assertThat(gb.scheduledEnd()).isEqualTo(PROJECT_START.plusDays(7));
    }

    @Test
    @DisplayName("No tasks in project returns empty list")
    void testNoTasks() {
        when(projectRepo.findById(PROJECT_ID)).thenReturn(Optional.of(project()));
        when(taskRepo.findAllByProjectIdOrderByDueDateAsc(PROJECT_ID)).thenReturn(Collections.emptyList());
        when(depRepo.findAllByProjectId(PROJECT_ID)).thenReturn(Collections.emptyList());

        List<GanttTaskItem> result = service.calculateTaskDates(PROJECT_ID);
        assertThat(result).isEmpty();
    }
}
