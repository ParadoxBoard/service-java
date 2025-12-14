package com.paradox.service_java.controller;

import com.paradox.service_java.dto.RepositoryDetailResponse;
import com.paradox.service_java.dto.RepositoryResponse;
import com.paradox.service_java.service.RepositoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * Controlador para repositorios
 * DEV A (Adrian)
 */
@RestController
@RequestMapping("/api/repos")
@Tag(name = "Repositories (Basic)", description = "Repository basic endpoints (DEV A)")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class RepositoryController {

    private final RepositoryService repositoryService;

    @Operation(
        summary = "Get user repositories",
        description = "Returns all repositories of the authenticated user based on their GitHub installation"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Repositories retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/user")
    public ResponseEntity<List<RepositoryResponse>> getUserRepositories(Principal principal) {
        String userEmail = principal != null ? principal.getName() : "test@example.com"; // TODO: Get from JWT
        List<RepositoryResponse> repos = repositoryService.findAllByUserEmail(userEmail);
        return ResponseEntity.ok(repos);
    }

    @Operation(
        summary = "Get repository details",
        description = "Returns detailed information about a repository including statistics (branches, commits, issues, PRs)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Repository found"),
        @ApiResponse(responseCode = "404", description = "Repository not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{repoId}")
    public ResponseEntity<RepositoryDetailResponse> getRepositoryDetails(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable UUID repoId) {

        return repositoryService.findByIdWithStats(repoId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

