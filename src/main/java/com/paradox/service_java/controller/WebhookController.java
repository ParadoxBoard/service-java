package com.paradox.service_java.controller;

import com.paradox.service_java.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for handling GitHub webhooks
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/github")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "GitHub webhook event handlers")
public class WebhookController {

    private final WebhookService webhookService;

    /**
     * Endpoint to receive GitHub webhook events
     * POST /webhooks/github
     */
    @Operation(summary = "Receive GitHub webhook events",
               description = "Handles GitHub webhook events with HMAC SHA-256 signature validation. " +
                           "Processes events like installation, push, pull_request, issues, etc.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Webhook processed successfully",
                    content = @Content(examples = @ExampleObject(value = "{\"status\": \"success\"}"))),
        @ApiResponse(responseCode = "401", description = "Invalid webhook signature",
                    content = @Content(examples = @ExampleObject(value = "{\"error\": \"invalid signature\"}"))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<Map<String, String>> handleWebhook(
            @Parameter(description = "GitHub webhook signature (HMAC SHA-256)", required = true)
            @RequestHeader("X-Hub-Signature-256") String signature,
            @Parameter(description = "GitHub event type (push, pull_request, installation, etc.)", required = true)
            @RequestHeader("X-GitHub-Event") String eventType,
            @Parameter(description = "GitHub delivery ID for tracking")
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "GitHub webhook payload (JSON)",
                required = true
            )
            @RequestBody String payload
    ) {
        log.info("Received GitHub webhook - Event: {}, Delivery: {}", eventType, deliveryId);

        try {
            // Validate signature
            if (!webhookService.validateSignature(payload, signature)) {
                log.warn("Invalid webhook signature for delivery: {}", deliveryId);
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "invalid signature"));
            }

            // Process webhook event
            webhookService.processWebhook(eventType, payload);

            log.info("Successfully processed webhook - Event: {}, Delivery: {}", eventType, deliveryId);
            return ResponseEntity.ok(Map.of("status", "success"));

        } catch (Exception e) {
            log.error("Error processing webhook - Event: {}, Delivery: {}, Error: {}", 
                    eventType, deliveryId, e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "internal server error", "message", e.getMessage()));
        }
    }

    /**
     * Health check endpoint for webhooks
     */
    @Operation(summary = "Webhook health check", description = "Simple health check endpoint for webhooks")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service is healthy")
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}

