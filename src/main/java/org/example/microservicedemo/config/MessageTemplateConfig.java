package org.example.microservicedemo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Configuration class for managing message templates for each service
 */
@Configuration
@ConfigurationProperties(prefix = "message")
@Data
public class MessageTemplateConfig {

    private ServiceTemplate serviceA = new ServiceTemplate();
    private ServiceTemplate serviceB = new ServiceTemplate();
    private ServiceTemplate serviceC = new ServiceTemplate();

    @Data
    public static class ServiceTemplate {
        private String template;
        private String serviceName;
        private String description;

        // Using AtomicReference for thread-safe runtime updates
        private final AtomicReference<String> runtimeTemplate = new AtomicReference<>();

        /**
         * Get the current template (runtime override or default)
         */
        public String getCurrentTemplate() {
            String runtime = runtimeTemplate.get();
            return runtime != null ? runtime : template;
        }

        /**
         * Update the template at runtime
         */
        public void updateTemplate(String newTemplate) {
            runtimeTemplate.set(newTemplate);
        }

        /**
         * Reset to the default template from configuration
         */
        public void resetToDefault() {
            runtimeTemplate.set(null);
        }
    }
}
