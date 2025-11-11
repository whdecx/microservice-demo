package org.example.microservicedemo.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Application-level configuration
 * Stores application metadata like name
 */
@Configuration
@Getter
public class AppConfig {

    @Value("${app.name}")
    private String applicationName;
}
