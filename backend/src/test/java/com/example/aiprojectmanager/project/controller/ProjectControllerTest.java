package com.example.aiprojectmanager.project.controller;

import com.example.aiprojectmanager.auth.JwtService;
import com.example.aiprojectmanager.project.dto.ProjectResponse;
import com.example.aiprojectmanager.project.service.ProjectService;
import com.example.aiprojectmanager.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(ProjectController.class)
class ProjectControllerTest {
    @Autowired MockMvc mvc;
    @MockBean ProjectService service;
    @MockBean JwtService jwtService;
    @MockBean UserRepository users;

    @Test
    @WithMockUser(username="demo@example.com")
    void createsProject() throws Exception {
        when(service.createProject(any())).thenReturn(new ProjectResponse(5L,1L,"Launch",null,null,null,null,null,com.example.aiprojectmanager.project.domain.ProjectStatus.DRAFT,null,null));
        mvc.perform(post("/api/v1/projects").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Launch\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(5)).andExpect(jsonPath("$.name").value("Launch"));
    }
}
