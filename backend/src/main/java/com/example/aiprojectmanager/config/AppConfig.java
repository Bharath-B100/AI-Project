package com.example.aiprojectmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Application-level Spring beans shared across modules.
 */
@Configuration
public class AppConfig {

    /**
     * RestTemplate bean used by IntegrationService to call external APIs
     * (GitHub, Jira, Asana, Monday.com, etc.).
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
