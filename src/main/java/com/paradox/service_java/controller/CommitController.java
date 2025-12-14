package com.paradox.service_java.controller;

import com.paradox.service_java.dto.CommitDetailResponse;
import com.paradox.service_java.dto.CommitResponse;
import com.paradox.service_java.dto.PaginatedResponse;
import com.paradox.service_java.service.CommitBasicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Controlador para commits
 * DEV A (Adrian)
 */
@RestController
@RequestMapping("/api/commits")
@Tag(name = "Commits", description = "Commit history and filtering operations")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class CommitController {

    private final CommitBasicService commitBasicService;

    @Operation(
        summary = "Get commits with filters",
        description = "Returns commits filtered by repository, branch, author and date range with pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Commits retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public ResponseEntity<PaginatedResponse<CommitResponse>> getCommits(
            @Parameter(description = "Repository ID", required = false)
            @RequestParam(required = false) UUID repoId,

            @Parameter(description = "Branch ID", required = false)
            @RequestParam(required = false) UUID branchId,

            @Parameter(description = "Author login", required = false)
            @RequestParam(required = false) String author,

            @Parameter(description = "Start date (ISO format)", required = false)
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,

            @Parameter(description = "End date (ISO format)", required = false)
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,

            @Parameter(description = "Page number (0-based)", required = false)
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", required = false)
            @RequestParam(defaultValue = "20") int size) {

        PaginatedResponse<CommitResponse> commits = commitBasicService.findByFilters(
                repoId, branchId, author, from, to, page, size);
        return ResponseEntity.ok(commits);
    }

    @Operation(
        summary = "Get commit details by SHA",
        description = "Returns detailed information about a specific commit"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Commit found"),
        @ApiResponse(responseCode = "404", description = "Commit not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/commit/{sha}")
    public ResponseEntity<CommitDetailResponse> getCommitDetails(
            @Parameter(description = "Commit SHA", required = true)
            @PathVariable String sha) {

        return commitBasicService.findByShaWithDetails(sha)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

