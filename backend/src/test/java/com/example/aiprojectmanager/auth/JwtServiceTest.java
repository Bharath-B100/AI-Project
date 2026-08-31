package com.example.aiprojectmanager.auth;

import com.example.aiprojectmanager.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKeyString", "9a4f2c5e7b3d1c8f6e0b4d9a2f1c8e3b5d6c7b9e0f2d4c5a6b8d9e0f1c2a3b4c");
    }

    @Test
    void generatesAndExtractsTokenSuccessfully() {
        User user = new User();
        user.setId(123L);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setRoles("ROLE_USER");

        String token = jwtService.generateToken(user);
        assertThat(token).isNotEmpty();

        String username = jwtService.extractUsername(token);
        assertThat(username).isEqualTo("test@example.com");
    }
}
