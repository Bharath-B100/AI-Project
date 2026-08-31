package com.example.aiprojectmanager.task.controller;

import com.example.aiprojectmanager.auth.JwtService;
import com.example.aiprojectmanager.task.domain.*;
import com.example.aiprojectmanager.task.dto.*;
import com.example.aiprojectmanager.task.service.TaskService;
import com.example.aiprojectmanager.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(TaskController.class)
class TaskControllerTest {
    @Autowired MockMvc mvc;
    @MockBean TaskService service;
    @MockBean JwtService jwtService;
    @MockBean UserRepository users;

    @Test
    @WithMockUser(username="demo@example.com")
    void createsTask() throws Exception {
        TaskResponse response = new TaskResponse(
            2L, 9L, "Design API", "Description", TaskStatus.TODO, TaskPriority.MEDIUM,
            BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now(), LocalDate.now(), 0,
            LocalDateTime.now(), LocalDateTime.now()
        );
        when(service.createTask(eq(9L), any())).thenReturn(response);

        mvc.perform(post("/api/v1/projects/9/tasks").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Design API\",\"description\":\"Description\",\"status\":\"TODO\",\"priority\":\"MEDIUM\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.title").value("Design API"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    @WithMockUser(username="demo@example.com")
    void listsTasks() throws Exception {
        TaskSummaryResponse task = new TaskSummaryResponse(2L, "Design API", TaskStatus.TODO, TaskPriority.MEDIUM, LocalDate.now(), 0);
        when(service.listTasksForProject(eq(9L))).thenReturn(List.of(task));

        mvc.perform(get("/api/v1/projects/9/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].title").value("Design API"));
    }

    @Test
    @WithMockUser(username="demo@example.com")
    void getsTask() throws Exception {
        TaskResponse response = new TaskResponse(
            2L, 9L, "Design API", "Description", TaskStatus.TODO, TaskPriority.MEDIUM,
            BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now(), LocalDate.now(), 0,
            LocalDateTime.now(), LocalDateTime.now()
        );
        when(service.getTaskById(eq(2L))).thenReturn(response);

        mvc.perform(get("/api/v1/tasks/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.title").value("Design API"));
    }

    @Test
    @WithMockUser(username="demo@example.com")
    void updatesTask() throws Exception {
        TaskResponse response = new TaskResponse(
            2L, 9L, "Design API V2", "Description", TaskStatus.IN_PROGRESS, TaskPriority.HIGH,
            BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now(), LocalDate.now(), 20,
            LocalDateTime.now(), LocalDateTime.now()
        );
        when(service.updateTask(eq(2L), any())).thenReturn(response);

        mvc.perform(put("/api/v1/tasks/2").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Design API V2\",\"description\":\"Description\",\"status\":\"IN_PROGRESS\",\"priority\":\"HIGH\",\"progressPercentage\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Design API V2"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.progressPercentage").value(20));
    }

    @Test
    @WithMockUser(username="demo@example.com")
    void patchesStatus() throws Exception {
        TaskResponse response = new TaskResponse(
            2L, 9L, "Design API", "Description", TaskStatus.DONE, TaskPriority.MEDIUM,
            BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now(), LocalDate.now(), 100,
            LocalDateTime.now(), LocalDateTime.now()
        );
        when(service.changeTaskStatus(eq(2L), eq(TaskStatus.DONE))).thenReturn(response);

        mvc.perform(patch("/api/v1/tasks/2/status").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.progressPercentage").value(100));
    }

    @Test
    @WithMockUser(username="demo@example.com")
    void patchesPriority() throws Exception {
        TaskResponse response = new TaskResponse(
            2L, 9L, "Design API", "Description", TaskStatus.TODO, TaskPriority.CRITICAL,
            BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now(), LocalDate.now(), 0,
            LocalDateTime.now(), LocalDateTime.now()
        );
        when(service.changeTaskPriority(eq(2L), eq(TaskPriority.CRITICAL))).thenReturn(response);

        mvc.perform(patch("/api/v1/tasks/2/priority").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"priority\":\"CRITICAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("CRITICAL"));
    }
}
