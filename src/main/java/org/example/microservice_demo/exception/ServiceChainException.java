package org.example.microservice_demo.exception;

import lombok.Getter;

/**
 * Custom exception for service chain failures
 */
@Getter
public class ServiceChainException extends RuntimeException {

    private final String failedService;
    private final String partialMessage;

    public ServiceChainException(String message, String failedService, String partialMessage) {
        super(message);
        this.failedService = failedService;
        this.partialMessage = partialMessage;
    }

    public ServiceChainException(String message, String failedService, String partialMessage, Throwable cause) {
        super(message, cause);
        this.failedService = failedService;
        this.partialMessage = partialMessage;
    }
}
