package com.example.aiprojectmanager.task.service;

import com.example.aiprojectmanager.auth.CurrentUserService;
import com.example.aiprojectmanager.project.domain.Project;
import com.example.aiprojectmanager.project.repository.ProjectRepository;
import com.example.aiprojectmanager.task.domain.*;
import com.example.aiprojectmanager.task.dto.CreateTaskRequest;
import com.example.aiprojectmanager.task.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class TaskServiceTest {
    @Mock TaskRepository tasks;
    @Mock ProjectRepository projects;
    @Mock CurrentUserService currentUserService;
    @InjectMocks TaskService service;

    private Project ownedProject() { Project p=new Project();p.setId(9L);p.setOwnerId(3L);return p; }
    @Test void createsAndListsTasksForOwnedProject() {
        when(currentUserService.getCurrentUserId()).thenReturn(3L);
        when(projects.findById(9L)).thenReturn(Optional.of(ownedProject()));
        when(tasks.save(any(Task.class))).thenAnswer(i->{Task t=i.getArgument(0);t.setId(2L);return t;});
        var created=service.createTask(9L,new CreateTaskRequest("Design",null,null,null,null,null,null,null,null));
        assertThat(created.projectId()).isEqualTo(9L); assertThat(created.status()).isEqualTo(TaskStatus.TODO);
        Task existing=new Task();existing.setId(2L);existing.setTitle("Design");existing.setStatus(TaskStatus.TODO);existing.setPriority(TaskPriority.MEDIUM);existing.setProgressPercentage(0);
        when(tasks.findAllByProjectIdOrderByDueDateAsc(9L)).thenReturn(List.of(existing));
        assertThat(service.listTasksForProject(9L)).hasSize(1);
    }
    @Test void changesStatusAndPriority() {
        when(currentUserService.getCurrentUserId()).thenReturn(3L);
        Task task=new Task();task.setId(2L);task.setProjectId(9L);task.setStatus(TaskStatus.TODO);task.setPriority(TaskPriority.MEDIUM);task.setProgressPercentage(0);
        when(tasks.findById(2L)).thenReturn(Optional.of(task));when(projects.findById(9L)).thenReturn(Optional.of(ownedProject()));
        assertThat(service.changeTaskStatus(2L,TaskStatus.DONE).progressPercentage()).isEqualTo(100);
        assertThat(service.changeTaskPriority(2L,TaskPriority.CRITICAL).priority()).isEqualTo(TaskPriority.CRITICAL);
    }
}
