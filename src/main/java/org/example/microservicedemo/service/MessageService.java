package org.example.microservicedemo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.microservicedemo.client.ServiceBClient;
import org.example.microservicedemo.client.ServiceCClient;
import org.example.microservicedemo.config.AppConfig;
import org.example.microservicedemo.config.MessageTemplateConfig;
import org.example.microservicedemo.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class handling message chain logic for all three services
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageTemplateConfig config;
    private final ServiceBClient serviceBClient;
    private final ServiceCClient serviceCClient;
    private final AppConfig appConfig;

    @Value("${services.use-rest-client:true}")
    private boolean useRestClient;

    @Value("${services.use-async:true}")
    private boolean useAsync;

    /**
     * Service A: Entry point - generates message and initiates chain
     */
    public MessageResponse processServiceA(String user, String ipAddress) {
        long startTime = System.currentTimeMillis();

        log.info("Service A: Processing request for user={} from IP={}", user, ipAddress);

        // Generate Service A's message
        String template = config.getServiceA().getCurrentTemplate();
        String serviceAMessage = template.replace("{user}", user);

        Instant serviceATimestamp = Instant.now();
        ChainLink serviceALink = ChainLink.builder()
                .service("service-a")
                .contribution(serviceAMessage)
                .timestamp(serviceATimestamp)
                .ipAddress(ipAddress)
                .build();

        // Call Service B (via RestClient or in-process)
        ServiceBRequest serviceBRequest = ServiceBRequest.builder()
                .currentMessage(serviceAMessage)
                .build();

        ServiceBResponse serviceBResponse;
        if (useRestClient) {
            if (useAsync) {
                log.info("Calling Service B via RestClient asynchronously");
                try {
                    serviceBResponse = serviceBClient.processMessageAsync(serviceBRequest).join();
                } catch (Exception e) {
                    log.error("Async call to Service B failed: {}", e.getMessage(), e);
                    throw new RuntimeException("Failed to process Service B asynchronously", e);
                }
            } else {
                log.info("Calling Service B via RestClient synchronously");
                serviceBResponse = serviceBClient.processMessage(serviceBRequest);
            }
        } else {
            log.info("Calling Service B via direct method call");
            serviceBResponse = processServiceB(serviceBRequest, "localhost");
        }

        // Build complete chain
        List<ChainLink> completeChain = new ArrayList<>();
        completeChain.add(serviceALink);
        completeChain.addAll(serviceBResponse.getChain());

        long processingTime = System.currentTimeMillis() - startTime;

        log.info("Service A: Complete message chain processed in {}ms", processingTime);

        return MessageResponse.builder()
                .applicationName(appConfig.getApplicationName())
                .message(serviceBResponse.getMessage())
                .chain(completeChain)
                .complete(true)
                .totalLength(serviceBResponse.getMessage().length())
                .processingTimeMs(processingTime)
                .build();
    }

    /**
     * Service B: Appends message and calls Service C
     */
    public ServiceBResponse processServiceB(ServiceBRequest request, String ipAddress) {
        log.info("Service B: Processing request with current message length={} from IP={}",
                request.getCurrentMessage().length(), ipAddress);

        // Get Service B's template and append message
        String template = config.getServiceB().getCurrentTemplate();
        String serviceBMessage = template.replace("{previous_message}", request.getCurrentMessage());

        // Extract the contribution by removing the placeholder
        String contribution = template.replace("{previous_message}", "").trim();

        Instant serviceBTimestamp = Instant.now();
        ChainLink serviceBLink = ChainLink.builder()
                .service("service-b")
                .contribution(contribution)
                .timestamp(serviceBTimestamp)
                .ipAddress(ipAddress)
                .build();

        // Call Service C (via RestClient or in-process)
        ServiceCRequest serviceCRequest = ServiceCRequest.builder()
                .currentMessage(serviceBMessage)
                .build();

        ServiceCResponse serviceCResponse;
        if (useRestClient) {
            if (useAsync) {
                log.info("Calling Service C via RestClient asynchronously");
                try {
                    serviceCResponse = serviceCClient.processMessageAsync(serviceCRequest).join();
                } catch (Exception e) {
                    log.error("Async call to Service C failed: {}", e.getMessage(), e);
                    throw new RuntimeException("Failed to process Service C asynchronously", e);
                }
            } else {
                log.info("Calling Service C via RestClient synchronously");
                serviceCResponse = serviceCClient.processMessage(serviceCRequest);
            }
        } else {
            log.info("Calling Service C via direct method call");
            serviceCResponse = processServiceC(serviceCRequest, "localhost");
        }

        // Build chain for Service B's response
        List<ChainLink> chain = new ArrayList<>();
        chain.add(serviceBLink);
        chain.add(ChainLink.builder()
                .service("service-c")
                .contribution(serviceCResponse.getContribution())
                .timestamp(serviceCResponse.getTimestamp())
                .ipAddress(serviceCResponse.getIpAddress())
                .build());

        log.info("Service B: Processed and forwarded to Service C");

        return ServiceBResponse.builder()
                .applicationName(appConfig.getApplicationName())
                .message(serviceCResponse.getMessage())
                .chain(chain)
                .build();
    }

    /**
     * Service C: Final service - appends final message and returns
     */
    public ServiceCResponse processServiceC(ServiceCRequest request, String ipAddress) {
        log.info("Service C: Processing final request with current message length={} from IP={}",
                request.getCurrentMessage().length(), ipAddress);

        // Get Service C's template and append final message
        String template = config.getServiceC().getCurrentTemplate();
        String finalMessage = template.replace("{previous_message}", request.getCurrentMessage());

        // Extract the contribution by removing the placeholder
        String contribution = template.replace("{previous_message}", "").trim();

        Instant serviceCTimestamp = Instant.now();

        log.info("Service C: Final message generated");

        return ServiceCResponse.builder()
                .applicationName(appConfig.getApplicationName())
                .message(finalMessage)
                .contribution(contribution)
                .timestamp(serviceCTimestamp)
                .ipAddress(ipAddress)
                .build();
    }

    /**
     * Update template for a specific service
     */
    public UpdateTemplateResponse updateTemplate(String serviceName, String newTemplate) {
        log.info("Updating template for service={}", serviceName);

        MessageTemplateConfig.ServiceTemplate serviceTemplate;

        switch (serviceName.toLowerCase()) {
            case "service-a":
                serviceTemplate = config.getServiceA();
                break;
            case "service-b":
                serviceTemplate = config.getServiceB();
                break;
            case "service-c":
                serviceTemplate = config.getServiceC();
                break;
            default:
                throw new IllegalArgumentException("Unknown service: " + serviceName);
        }

        serviceTemplate.updateTemplate(newTemplate);

        log.info("Template updated successfully for service={}", serviceName);

        return UpdateTemplateResponse.builder()
                .applicationName(appConfig.getApplicationName())
                .service(serviceName)
                .template(newTemplate)
                .updatedAt(Instant.now())
                .message("Message template updated successfully")
                .build();
    }

    /**
     * Get current template for a service
     */
    public String getCurrentTemplate(String serviceName) {
        switch (serviceName.toLowerCase()) {
            case "service-a":
                return config.getServiceA().getCurrentTemplate();
            case "service-b":
                return config.getServiceB().getCurrentTemplate();
            case "service-c":
                return config.getServiceC().getCurrentTemplate();
            default:
                throw new IllegalArgumentException("Unknown service: " + serviceName);
        }
    }
}
