package com.paradox.service_java.controller;

import com.paradox.service_java.dto.PullRequestReviewResponse;
import com.paradox.service_java.dto.PullRequestSimpleResponse;
import com.paradox.service_java.service.PullRequestAdvancedService;
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
 * Controlador avanzado para Pull Requests
 * Responsabilidad: DEV B (Isabella)
 */
@RestController
@RequestMapping("/api/prs")
@Tag(name = "Pull Requests", description = "Pull request filtering and review operations")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class PullRequestAdvancedController {

    private final PullRequestAdvancedService pullRequestAdvancedService;

    @Operation(
        summary = "Get open pull requests",
        description = "Returns only open (not closed or merged) pull requests from a repository"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pull requests retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Repository not found")
    })
    @GetMapping("/open")
    public ResponseEntity<List<PullRequestSimpleResponse>> getOpenPullRequests(
            @Parameter(description = "Repository ID", required = true)
            @RequestParam UUID repoId) {
        List<PullRequestSimpleResponse> prs = pullRequestAdvancedService.getOpenPullRequests(repoId);
        return ResponseEntity.ok(prs);
    }

    @Operation(
        summary = "Get pull request reviews",
        description = "Returns all reviews for a specific pull request from GitHub API"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reviews retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Pull request not found")
    })
    @GetMapping("/{number}/reviews")
    public ResponseEntity<List<PullRequestReviewResponse>> getPullRequestReviews(
            @Parameter(description = "Pull request number", required = true)
            @PathVariable Integer number,
            @Parameter(description = "Repository ID", required = true)
            @RequestParam UUID repoId,
            @Parameter(description = "GitHub Installation ID", required = true)
            @RequestParam Long installationId) {
        List<PullRequestReviewResponse> reviews = pullRequestAdvancedService.getPullRequestReviews(
                repoId, number, installationId
        );
        return ResponseEntity.ok(reviews);
    }
}

