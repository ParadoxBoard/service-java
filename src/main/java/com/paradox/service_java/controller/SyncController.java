package com.paradox.service_java.controller;

import com.paradox.service_java.service.IncrementalSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para sincronización incremental
 * Responsabilidad: DEV B (Isabella)
 */
@Slf4j
@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
@Tag(name = "Synchronization", description = "Incremental synchronization endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class SyncController {

    private final IncrementalSyncService incrementalSyncService;

    /**
     * Endpoint para sincronización completa de una instalación
     * POST /api/sync/full?installationId={id}
     */
    @Operation(
        summary = "Full synchronization",
        description = "Performs a full incremental sync for a GitHub installation. " +
                     "Syncs all Pull Requests, Issues, and detects changes since last sync. " +
                     "Returns a detailed summary of synced items."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sync completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid installation ID"),
        @ApiResponse(responseCode = "404", description = "Installation not found"),
        @ApiResponse(responseCode = "500", description = "Sync failed")
    })
    @PostMapping("/full")
    public ResponseEntity<IncrementalSyncService.SyncSummary> syncFull(
            @Parameter(description = "GitHub Installation ID", required = true)
            @RequestParam Long installationId) {

        log.info("Received request for full sync of installation: {}", installationId);

        try {
            IncrementalSyncService.SyncSummary summary = incrementalSyncService.syncFull(installationId);

            if (summary.isSuccess()) {
                log.info("Full sync completed successfully for installation {}: {}", installationId, summary);
                return ResponseEntity.ok(summary);
            } else {
                log.error("Full sync failed for installation {}: {}", installationId, summary.getErrors());
                return ResponseEntity.status(500).body(summary);
            }

        } catch (Exception e) {
            log.error("Error in full sync for installation {}: {}", installationId, e.getMessage(), e);

            IncrementalSyncService.SyncSummary errorSummary = new IncrementalSyncService.SyncSummary();
            errorSummary.setInstallationId(installationId);
            errorSummary.setSuccess(false);
            errorSummary.addError("GLOBAL", e.getMessage());

            return ResponseEntity.status(500).body(errorSummary);
        }
    }

    /**
     * Endpoint para obtener resumen de sincronización
     * GET /api/sync/summary?installationId={id}
     */
    @Operation(
        summary = "Get sync summary",
        description = "Returns a summary of the last synchronization for an installation. " +
                     "Useful for monitoring sync status and detecting issues."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Summary retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "No sync history found")
    })
    @GetMapping("/summary")
    public ResponseEntity<SyncSummaryResponse> getSyncSummary(
            @Parameter(description = "GitHub Installation ID", required = true)
            @RequestParam Long installationId) {

        log.info("Received request for sync summary of installation: {}", installationId);

        // TODO: Implementar almacenamiento de historial de sincronizaciones
        // Por ahora retornamos un placeholder
        SyncSummaryResponse response = new SyncSummaryResponse();
        response.setInstallationId(installationId);
        response.setMessage("Sync summary not yet implemented. Use /api/sync/full to perform a sync.");

        return ResponseEntity.ok(response);
    }

    /**
     * DTO para respuesta de resumen de sincronización
     */
    public static class SyncSummaryResponse {
        private Long installationId;
        private String message;
        private Integer totalSyncs;
        private String lastSyncDate;

        public Long getInstallationId() { return installationId; }
        public void setInstallationId(Long installationId) { this.installationId = installationId; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Integer getTotalSyncs() { return totalSyncs; }
        public void setTotalSyncs(Integer totalSyncs) { this.totalSyncs = totalSyncs; }

        public String getLastSyncDate() { return lastSyncDate; }
        public void setLastSyncDate(String lastSyncDate) { this.lastSyncDate = lastSyncDate; }
    }
}

