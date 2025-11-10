package org.example.microservicedemo.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request model for Service B internal API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceBRequest {
    @NotBlank(message = "current_message is required")
    private String currentMessage;
}
