package org.example.microservicedemo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents a single service's contribution in the message chain
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainLink {
    private String service;
    private String contribution;
    private Instant timestamp;
    private String ipAddress;
}
