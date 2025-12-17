package com.paradox.service_java.controller;

import com.paradox.service_java.service.InstallationSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador para sincronización manual de instalaciones y repositorios
 */
@Slf4j
@RestController
@RequestMapping("/api/sync")
@Tag(name = "Sync", description = "Manual synchronization endpoints")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class InstallationSyncController {

    private final InstallationSyncService syncService;

    @Operation(
        summary = "Sync repositories from webhook logs",
        description = "Re-processes installation webhooks to sync repositories that weren't saved initially"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sync completed"),
        @ApiResponse(responseCode = "500", description = "Sync failed")
    })
    @PostMapping("/repositories/from-webhooks")
    public ResponseEntity<Map<String, Object>> syncRepositoriesFromWebhooks() {
        try {
            Map<String, Object> result = syncService.syncRepositoriesFromWebhookLogs();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error syncing repositories from webhooks: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(
        summary = "Sync repositories for specific installation",
        description = "Re-processes the latest installation webhook for a specific installation ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sync completed"),
        @ApiResponse(responseCode = "404", description = "Installation not found"),
        @ApiResponse(responseCode = "500", description = "Sync failed")
    })
    @PostMapping("/repositories/installation/{installationId}")
    public ResponseEntity<Map<String, Object>> syncRepositoriesForInstallation(
            @PathVariable Long installationId) {
        try {
            Map<String, Object> result = syncService.syncRepositoriesForInstallation(installationId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error syncing repositories for installation {}: {}", installationId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

