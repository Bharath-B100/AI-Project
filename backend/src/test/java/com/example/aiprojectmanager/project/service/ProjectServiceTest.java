package com.example.aiprojectmanager.project.service;

import com.example.aiprojectmanager.auth.CurrentUserService;
import com.example.aiprojectmanager.project.domain.Project;
import com.example.aiprojectmanager.project.dto.CreateProjectRequest;
import com.example.aiprojectmanager.project.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ProjectServiceTest {
    @Mock ProjectRepository projects;
    @Mock CurrentUserService currentUserService;
    @InjectMocks ProjectService service;

    @Test void createsProjectForCurrentUser() {
        when(currentUserService.getCurrentUserId()).thenReturn(3L);
        when(projects.save(any(Project.class))).thenAnswer(i -> { Project p=i.getArgument(0); p.setId(7L); return p; });
        var result = service.createProject(new CreateProjectRequest("Website", "New site", null, null, null, null, null));
        assertThat(result.id()).isEqualTo(7L); assertThat(result.ownerId()).isEqualTo(3L); assertThat(result.status()).isEqualTo(com.example.aiprojectmanager.project.domain.ProjectStatus.DRAFT);
    }
    @Test void listsOnlyUsersProjects() {
        when(currentUserService.getCurrentUserId()).thenReturn(3L);
        Project p = new Project(); p.setId(1L); p.setName("Mine");
        when(projects.findAllByOwnerIdOrderByUpdatedAtDesc(3L)).thenReturn(List.of(p));
        assertThat(service.listProjectsForUser().stream().map(r -> r.name()).toList()).containsExactly("Mine");
        verify(projects).findAllByOwnerIdOrderByUpdatedAtDesc(3L);
    }
}
