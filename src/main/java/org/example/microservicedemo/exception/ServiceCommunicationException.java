package org.example.microservicedemo.exception;

/**
 * Exception thrown when inter-service communication fails
 */
public class ServiceCommunicationException extends RuntimeException {

    public ServiceCommunicationException(String message) {
        super(message);
    }

    public ServiceCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
