package org.example.microservice_demo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.microservice_demo.model.ServiceBRequest;
import org.example.microservice_demo.model.ServiceBResponse;
import org.example.microservice_demo.model.UpdateTemplateRequest;
import org.example.microservice_demo.model.UpdateTemplateResponse;
import org.example.microservice_demo.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Service B Controller - Internal API
 * Called by Service A, appends message and calls Service C
 */
@RestController
@RequestMapping("/internal/service-b")
@RequiredArgsConstructor
@Slf4j
public class ServiceBController {

    private final MessageService messageService;

    /**
     * POST /internal/service-b/append - Internal endpoint called by Service A
     *
     * @param request Contains current message from Service A
     * @return Combined message with Service B and C contributions
     */
    @PostMapping("/append")
    public ResponseEntity<ServiceBResponse> appendMessage(
            @RequestBody @Validated ServiceBRequest request,
            @RequestHeader(value = "X-Internal-Request", required = false) String internalHeader) {

        log.info("Service B: Received internal request");

        ServiceBResponse response = messageService.processServiceB(request);

        log.info("Service B: Returning response to Service A");

        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/service-b/message - Update Service B's message template
     *
     * @param request New template
     * @return Update confirmation
     */
    @PutMapping("/api/service-b/message")
    public ResponseEntity<UpdateTemplateResponse> updateServiceBTemplate(
            @RequestBody @Validated UpdateTemplateRequest request) {

        log.info("Received template update request for Service B");

        UpdateTemplateResponse response = messageService.updateTemplate("service-b", request.getTemplate());

        return ResponseEntity.ok(response);
    }
}
