package com.example.aiprojectmanager.auth;

import com.example.aiprojectmanager.user.domain.User;
import com.example.aiprojectmanager.user.repository.UserRepository;
import com.example.aiprojectmanager.project.domain.Project;
import com.example.aiprojectmanager.project.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void cleanDb() {
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registersUserSuccessfully() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Integration User\",\"email\":\"integration@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("integration@example.com"))
                .andExpect(jsonPath("$.user.name").value("Integration User"));

        assertThat(userRepository.findByEmail("integration@example.com")).isPresent();
    }

    @Test
    void loginsUserSuccessfully() throws Exception {
        User user = new User();
        user.setName("Login User");
        user.setEmail("login@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRoles("ROLE_USER");
        userRepository.save(user);

        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"login@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("login@example.com"));
    }

    @Test
    void loginFailsWithInvalidCredentials() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"invalid@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessingProtectedEndpointWithoutTokenFails() throws Exception {
        mvc.perform(get("/api/v1/projects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessingProtectedEndpointWithValidTokenSucceeds() throws Exception {
        User user = new User();
        user.setName("Token User");
        user.setEmail("token@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRoles("ROLE_USER");
        user = userRepository.save(user);

        String token = jwtService.generateToken(user);

        mvc.perform(get("/api/v1/projects")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void accessingAnotherUsersProjectReturnsForbidden() throws Exception {
        // User A
        User userA = new User();
        userA.setName("User A");
        userA.setEmail("usera@example.com");
        userA.setPasswordHash(passwordEncoder.encode("password"));
        userA.setRoles("ROLE_USER");
        userA = userRepository.save(userA);

        // User B
        User userB = new User();
        userB.setName("User B");
        userB.setEmail("userb@example.com");
        userB.setPasswordHash(passwordEncoder.encode("password"));
        userB.setRoles("ROLE_USER");
        userB = userRepository.save(userB);

        // Project owned by User B
        Project project = new Project();
        project.setOwnerId(userB.getId());
        project.setName("User B's Project");
        project.setStatus(com.example.aiprojectmanager.project.domain.ProjectStatus.ACTIVE);
        project = projectRepository.save(project);

        // User A tries to view User B's project
        String tokenA = jwtService.generateToken(userA);

        mvc.perform(get("/api/v1/projects/" + project.getId())
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());
    }
}
