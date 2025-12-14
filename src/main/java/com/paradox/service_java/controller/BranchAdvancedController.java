package com.paradox.service_java.controller;

import com.paradox.service_java.dto.BranchChangeResponse;
import com.paradox.service_java.dto.BranchProtectionResponse;
import com.paradox.service_java.service.BranchAdvancedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador avanzado para branches
 * Responsabilidad: DEV B (Isabella)
 */
@RestController
@RequestMapping("/api/branches")
@Tag(name = "Branches (Advanced)", description = "Advanced branch endpoints (DEV B)")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class BranchAdvancedController {

    private final BranchAdvancedService branchAdvancedService;

    @Operation(
        summary = "Get recent branch changes",
        description = "Returns branches with changes in the last 24 hours, including open PRs"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Changes retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Repository not found")
    })
    @GetMapping("/changes")
    public ResponseEntity<List<BranchChangeResponse>> getRecentChanges(
            @Parameter(description = "Repository ID", required = true)
            @RequestParam UUID repoId) {
        List<BranchChangeResponse> changes = branchAdvancedService.getRecentChanges(repoId);
        return ResponseEntity.ok(changes);
    }

    @Operation(
        summary = "Get branch protection configuration",
        description = "Returns the protection rules configured for a specific branch from GitHub API"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Protection config retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Branch not found")
    })
    @GetMapping("/{branchId}/protection")
    public ResponseEntity<BranchProtectionResponse> getBranchProtection(
            @Parameter(description = "Branch ID", required = true)
            @PathVariable UUID branchId,
            @Parameter(description = "GitHub Installation ID", required = true)
            @RequestParam Long installationId) {
        BranchProtectionResponse protection = branchAdvancedService.getBranchProtection(branchId, installationId);
        return ResponseEntity.ok(protection);
    }
}

