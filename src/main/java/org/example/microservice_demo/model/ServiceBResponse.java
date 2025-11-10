package org.example.microservice_demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response model for Service B internal API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceBResponse {
    private String message;
    private List<ChainLink> chain;
}
