package org.example.microservice_demo.controller;

import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.microservice_demo.model.MessageResponse;
import org.example.microservice_demo.model.UpdateTemplateRequest;
import org.example.microservice_demo.model.UpdateTemplateResponse;
import org.example.microservice_demo.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Service A Controller - Client-facing API
 * Entry point for the message chain
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ServiceAController {

    private final MessageService messageService;

    /**
     * GET /api/message - Main endpoint to get complete message chain
     *
     * @param user Username to personalize the message (default: "guest")
     * @return Complete message with chain details
     */
    @GetMapping("/message")
    public ResponseEntity<MessageResponse> getMessage(
            @RequestParam(defaultValue = "guest")
            @Size(max = 50, message = "Query parameter 'user' must not exceed 50 characters")
            String user) {

        log.info("Received request for user: {}", user);

        MessageResponse response = messageService.processServiceA(user);

        log.info("Returning complete message chain to client");

        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/service-a/message - Update Service A's message template
     *
     * @param request New template
     * @return Update confirmation
     */
    @PutMapping("/service-a/message")
    public ResponseEntity<UpdateTemplateResponse> updateServiceATemplate(
            @RequestBody @Validated UpdateTemplateRequest request) {

        log.info("Received template update request for Service A");

        UpdateTemplateResponse response = messageService.updateTemplate("service-a", request.getTemplate());

        return ResponseEntity.ok(response);
    }
}
