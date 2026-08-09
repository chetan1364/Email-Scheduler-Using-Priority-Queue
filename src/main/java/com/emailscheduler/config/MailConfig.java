package com.emailscheduler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class MailConfig {

    /**
     * RestTemplate used by MailDispatchService to call the Brevo HTTP API.
     * Spring Boot auto-configures this class; no SMTP configuration needed.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
