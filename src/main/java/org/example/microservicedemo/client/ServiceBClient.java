package org.example.microservicedemo.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.microservicedemo.exception.ServiceCommunicationException;
import org.example.microservicedemo.model.ServiceBRequest;
import org.example.microservicedemo.model.ServiceBResponse;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.concurrent.CompletableFuture;

/**
 * Client for communicating with Service B using RestClient with async support
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceBClient {

    private final RestClient serviceBRestClient;

    /**
     * Call Service B to process message asynchronously
     *
     * @param request Request containing current message
     * @return CompletableFuture with Response from Service B with updated message and chain
     * @throws ServiceCommunicationException if communication fails
     */
    @Async("asyncRestClientExecutor")
    public CompletableFuture<ServiceBResponse> processMessageAsync(ServiceBRequest request) {
        log.info("Calling Service B asynchronously with message length: {} on thread: {}",
                request.getCurrentMessage().length(), Thread.currentThread().getName());

        return CompletableFuture.supplyAsync(() -> {
            try {
                ServiceBResponse response = serviceBRestClient.post()
                        .uri("/internal/service-b/append")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Internal-Request", "true")
                        .body(request)
                        .retrieve()
                        .body(ServiceBResponse.class);

                if (response == null) {
                    throw new ServiceCommunicationException("Service B returned null response");
                }

                log.info("Successfully received async response from Service B on thread: {}",
                        Thread.currentThread().getName());
                return response;

            } catch (RestClientException e) {
                log.error("Failed to communicate with Service B asynchronously: {}", e.getMessage(), e);
                throw new ServiceCommunicationException(
                        "Failed to communicate with Service B: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Call Service B to process message synchronously (backward compatibility)
     *
     * @param request Request containing current message
     * @return Response from Service B with updated message and chain
     * @throws ServiceCommunicationException if communication fails
     */
    public ServiceBResponse processMessage(ServiceBRequest request) {
        log.info("Calling Service B synchronously with message length: {}", request.getCurrentMessage().length());

        try {
            ServiceBResponse response = serviceBRestClient.post()
                    .uri("/internal/service-b/append")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Internal-Request", "true")
                    .body(request)
                    .retrieve()
                    .body(ServiceBResponse.class);

            if (response == null) {
                throw new ServiceCommunicationException("Service B returned null response");
            }

            log.info("Successfully received response from Service B");
            return response;

        } catch (RestClientException e) {
            log.error("Failed to communicate with Service B: {}", e.getMessage(), e);
            throw new ServiceCommunicationException(
                    "Failed to communicate with Service B: " + e.getMessage(), e);
        }
    }
}