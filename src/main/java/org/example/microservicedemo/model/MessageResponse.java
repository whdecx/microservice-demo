package org.example.microservicedemo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response for the client-facing GET /api/message endpoint
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private String message;
    private List<ChainLink> chain;
    private Boolean complete;
    private Integer totalLength;
    private Long processingTimeMs;
}
