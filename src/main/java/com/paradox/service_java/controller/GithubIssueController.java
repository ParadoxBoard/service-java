package com.paradox.service_java.controller;

import com.paradox.service_java.dto.GithubIssueResponse;
import com.paradox.service_java.dto.PaginatedResponse;
import com.paradox.service_java.service.GithubIssueService;
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
 * Controlador para GitHub Issues
 * DEV A (Adrian)
 */
@RestController
@RequestMapping("/api/github/issues")
@Tag(name = "GitHub Issues (Basic)", description = "GitHub issue basic endpoints (DEV A)")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class GithubIssueController {

    private final GithubIssueService githubIssueService;

    @Operation(
        summary = "Get repository issues",
        description = "Returns GitHub issues from a repository with optional state filter and pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Issues retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Repository not found")
    })
    @GetMapping
    public ResponseEntity<PaginatedResponse<GithubIssueResponse>> getRepositoryIssues(
            @Parameter(description = "Repository ID", required = true)
            @RequestParam UUID repoId,

            @Parameter(description = "Issue state (open/closed)", required = false)
            @RequestParam(required = false) String state,

            @Parameter(description = "Page number (0-based)", required = false)
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", required = false)
            @RequestParam(defaultValue = "20") int size) {

        PaginatedResponse<GithubIssueResponse> issues = githubIssueService.findByRepoWithFilters(
                repoId, state, page, size);
        return ResponseEntity.ok(issues);
    }

    @Operation(
        summary = "Get issue details",
        description = "Returns detailed information about a specific GitHub issue"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Issue found"),
        @ApiResponse(responseCode = "404", description = "Issue not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/issue/{number}")
    public ResponseEntity<GithubIssueResponse> getIssueDetails(
            @Parameter(description = "Issue number", required = true)
            @PathVariable Integer number,

            @Parameter(description = "Repository ID", required = true)
            @RequestParam UUID repoId) {

        return githubIssueService.findByNumberAndRepo(number, repoId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

