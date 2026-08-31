package com.example.aiprojectmanager.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.aiprojectmanager.user.repository.UserRepository;
import com.example.aiprojectmanager.user.domain.User;

@Configuration
@Profile("!test")
public class DemoDataSeeder {

    @Bean
    CommandLineRunner seedDemoUser(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            if (users.findByEmail("demo@example.com").isEmpty()) {
                User u = new User();
                u.setName("Demo User");
                u.setEmail("demo@example.com");
                u.setPasswordHash(encoder.encode("password"));
                u.setRoles("ROLE_USER");
                users.save(u);
            }
        };
    }
}
