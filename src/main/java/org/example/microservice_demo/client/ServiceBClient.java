package org.example.microservice_demo.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.microservice_demo.exception.ServiceCommunicationException;
import org.example.microservice_demo.model.ServiceBRequest;
import org.example.microservice_demo.model.ServiceBResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Client for communicating with Service B using RestClient
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceBClient {

    private final RestClient serviceBRestClient;

    /**
     * Call Service B to process message
     *
     * @param request Request containing current message
     * @return Response from Service B with updated message and chain
     * @throws ServiceCommunicationException if communication fails
     */
    public ServiceBResponse processMessage(ServiceBRequest request) {
        log.info("Calling Service B with message length: {}", request.getCurrentMessage().length());

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