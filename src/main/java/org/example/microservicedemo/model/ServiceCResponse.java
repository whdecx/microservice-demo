package org.example.microservicedemo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response model for Service C internal API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCResponse {
    private String applicationName;
    private String message;
    private String contribution;
    private Instant timestamp;
    private String ipAddress;
}
