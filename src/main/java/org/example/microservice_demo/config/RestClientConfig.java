package org.example.microservice_demo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configuration for RestClient beans used for inter-service communication
 */
@Configuration
@Slf4j
public class RestClientConfig {

    @Value("${services.service-b.url:http://localhost:8081}")
    private String serviceBUrl;

    @Value("${services.service-c.url:http://localhost:8082}")
    private String serviceCUrl;

    @Value("${services.rest-client.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${services.rest-client.read-timeout:10000}")
    private int readTimeout;

    /**
     * RestClient for Service B
     */
    @Bean
    public RestClient serviceBRestClient(RestClient.Builder builder) {
        log.info("Creating RestClient for Service B with base URL: {}", serviceBUrl);

        return builder
                .baseUrl(serviceBUrl)
                .requestInterceptor(loggingInterceptor("Service-B"))
                .build();
    }

    /**
     * RestClient for Service C
     */
    @Bean
    public RestClient serviceCRestClient(RestClient.Builder builder) {
        log.info("Creating RestClient for Service C with base URL: {}", serviceCUrl);

        return builder
                .baseUrl(serviceCUrl)
                .requestInterceptor(loggingInterceptor("Service-C"))
                .build();
    }

    /**
     * Logging interceptor for debugging API calls
     */
    private ClientHttpRequestInterceptor loggingInterceptor(String serviceName) {
        return (request, body, execution) -> {
            log.debug("Request to {}: {} {}", serviceName, request.getMethod(), request.getURI());
            var response = execution.execute(request, body);
            log.debug("Response from {}: {}", serviceName, response.getStatusCode());
            return response;
        };
    }
}
