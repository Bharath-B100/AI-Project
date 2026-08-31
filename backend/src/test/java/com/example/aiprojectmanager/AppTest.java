package com.example.aiprojectmanager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AppTest {

    @Test
    void contextLoads() {
        // Verification test that the Spring application context boots successfully with H2 database.
    }
}
