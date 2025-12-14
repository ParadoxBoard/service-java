package com.paradox.service_java.controller;

import com.paradox.service_java.dto.PaginatedResponse;
import com.paradox.service_java.dto.PullRequestResponse;
import com.paradox.service_java.service.PullRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador para Pull Requests
 * DEV A (Adrian)
 */
@RestController
@RequestMapping("/api/prs")
@Tag(name = "Pull Requests (Basic)", description = "Pull Request basic endpoints (DEV A)")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class PullRequestController {

    private final PullRequestService pullRequestService;

    @Operation(
        summary = "Get repository pull requests",
        description = "Returns pull requests from a repository with optional filters (state, author) and pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pull requests retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Repository not found")
    })
    @GetMapping
    public ResponseEntity<PaginatedResponse<PullRequestResponse>> getRepositoryPRs(
            @Parameter(description = "Repository ID", required = true)
            @RequestParam UUID repoId,

            @Parameter(description = "PR state (open/closed/merged)", required = false)
            @RequestParam(required = false) String state,

            @Parameter(description = "Author login", required = false)
            @RequestParam(required = false) String author,

            @Parameter(description = "Page number (0-based)", required = false)
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", required = false)
            @RequestParam(defaultValue = "20") int size) {

        PaginatedResponse<PullRequestResponse> prs = pullRequestService.findByRepoWithFilters(
                repoId, state, author, page, size);
        return ResponseEntity.ok(prs);
    }

    @Operation(
        summary = "Get pull request details",
        description = "Returns detailed information about a specific pull request"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pull request found"),
        @ApiResponse(responseCode = "404", description = "Pull request not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/pr/{number}")
    public ResponseEntity<PullRequestResponse> getPRDetails(
            @Parameter(description = "PR number", required = true)
            @PathVariable Integer number,

            @Parameter(description = "Repository ID", required = true)
            @RequestParam UUID repoId) {

        return pullRequestService.findByNumberAndRepo(number, repoId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

