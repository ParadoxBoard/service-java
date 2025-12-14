package com.paradox.service_java.controller;

import com.paradox.service_java.dto.InstallationResponse;
import com.paradox.service_java.service.InstallationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

/**
 * Controlador para gestión de instalaciones de GitHub App
 */
@RestController
@RequestMapping("/api/user")
@Tag(name = "User Installations", description = "User GitHub App installation management")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class UserInstallationController {

    private final InstallationService installationService;

    @Operation(
        summary = "Get user's GitHub App installation",
        description = "Returns the GitHub App installation for the authenticated user (if exists)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Installation found or null"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/installation")
    public ResponseEntity<InstallationResponse> getUserInstallation(Principal principal) {
        String userEmail = principal.getName(); // Get from JWT

        Optional<InstallationResponse> installation = installationService.findByUserEmail(userEmail);

        return installation
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok().body(null)); // No installation = null
    }

    @Operation(
        summary = "Get all user's installations",
        description = "Returns all GitHub App installations (user + organizations)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Installations retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/installations")
    public ResponseEntity<List<InstallationResponse>> getAllUserInstallations(Principal principal) {
        String userEmail = principal.getName();

        List<InstallationResponse> installations = installationService
                .findAllByUserEmail(userEmail);

        return ResponseEntity.ok(installations);
    }

    @Operation(
        summary = "Check if user has GitHub App installed",
        description = "Returns true if user has at least one active installation"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Check completed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/has-installation")
    public ResponseEntity<Boolean> hasInstallation(Principal principal) {
        String userEmail = principal.getName();

        boolean hasInstallation = installationService
                .hasActiveInstallation(userEmail);

        return ResponseEntity.ok(hasInstallation);
    }
}

