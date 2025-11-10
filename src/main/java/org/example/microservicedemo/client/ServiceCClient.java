package org.example.microservicedemo.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.microservicedemo.exception.ServiceCommunicationException;
import org.example.microservicedemo.model.ServiceCRequest;
import org.example.microservicedemo.model.ServiceCResponse;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.concurrent.CompletableFuture;

/**
 * Client for communicating with Service C using RestClient with async support
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceCClient {

    private final RestClient serviceCRestClient;

    /**
     * Call Service C to process final message asynchronously
     *
     * @param request Request containing current message
     * @return CompletableFuture with Response from Service C with final message
     * @throws ServiceCommunicationException if communication fails
     */
    @Async("asyncRestClientExecutor")
    public CompletableFuture<ServiceCResponse> processMessageAsync(ServiceCRequest request) {
        log.info("Calling Service C asynchronously with message length: {} on thread: {}",
                request.getCurrentMessage().length(), Thread.currentThread().getName());

        return CompletableFuture.supplyAsync(() -> {
            try {
                ServiceCResponse response = serviceCRestClient.post()
                        .uri("/internal/service-c/finalize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Internal-Request", "true")
                        .body(request)
                        .retrieve()
                        .body(ServiceCResponse.class);

                if (response == null) {
                    throw new ServiceCommunicationException("Service C returned null response");
                }

                log.info("Successfully received async response from Service C on thread: {}",
                        Thread.currentThread().getName());
                return response;

            } catch (RestClientException e) {
                log.error("Failed to communicate with Service C asynchronously: {}", e.getMessage(), e);
                throw new ServiceCommunicationException(
                        "Failed to communicate with Service C: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Call Service C to process final message synchronously (backward compatibility)
     *
     * @param request Request containing current message
     * @return Response from Service C with final message
     * @throws ServiceCommunicationException if communication fails
     */
    public ServiceCResponse processMessage(ServiceCRequest request) {
        log.info("Calling Service C synchronously with message length: {}", request.getCurrentMessage().length());

        try {
            ServiceCResponse response = serviceCRestClient.post()
                    .uri("/internal/service-c/finalize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Internal-Request", "true")
                    .body(request)
                    .retrieve()
                    .body(ServiceCResponse.class);

            if (response == null) {
                throw new ServiceCommunicationException("Service C returned null response");
            }

            log.info("Successfully received response from Service C");
            return response;

        } catch (RestClientException e) {
            log.error("Failed to communicate with Service C: {}", e.getMessage(), e);
            throw new ServiceCommunicationException(
                    "Failed to communicate with Service C: " + e.getMessage(), e);
        }
    }
}
