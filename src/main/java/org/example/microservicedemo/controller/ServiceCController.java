package org.example.microservicedemo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.microservicedemo.model.ServiceCRequest;
import org.example.microservicedemo.model.ServiceCResponse;
import org.example.microservicedemo.model.UpdateTemplateRequest;
import org.example.microservicedemo.model.UpdateTemplateResponse;
import org.example.microservicedemo.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Service C Controller - Internal API (Final Service)
 * Called by Service B, appends final message and returns
 */
@RestController
@RequestMapping("/internal/service-c")
@RequiredArgsConstructor
@Slf4j
public class ServiceCController {

    private final MessageService messageService;

    /**
     * POST /internal/service-c/finalize - Internal endpoint called by Service B
     *
     * @param request Contains combined message from Service A and B
     * @return Final complete message
     */
    @PostMapping("/finalize")
    public ResponseEntity<ServiceCResponse> finalizeMessage(
            @RequestBody @Validated ServiceCRequest request,
            @RequestHeader(value = "X-Internal-Request", required = false) String internalHeader) {

        log.info("Service C: Received internal request for finalization");

        ServiceCResponse response = messageService.processServiceC(request);

        log.info("Service C: Returning final message to Service B");

        return ResponseEntity.ok(response);
    }

    /**
     * PUT /internal/service-c/message - Update Service C's message template
     *
     * @param request New template
     * @return Update confirmation
     */
    @PutMapping("/message")
    public ResponseEntity<UpdateTemplateResponse> updateServiceCTemplate(
            @RequestBody @Validated UpdateTemplateRequest request) {

        log.info("Received template update request for Service C");

        UpdateTemplateResponse response = messageService.updateTemplate("service-c", request.getTemplate());

        return ResponseEntity.ok(response);
    }
}
