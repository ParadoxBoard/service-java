package com.paradox.service_java.controller;

import com.paradox.service_java.dto.BranchDetailResponse;
import com.paradox.service_java.dto.BranchResponse;
import com.paradox.service_java.service.BranchBasicService;
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
 * Controlador para branches
 * DEV A (Adrian)
 */
@RestController
@RequestMapping("/api/branches")
@Tag(name = "Branches", description = "Branch management and commit history operations")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class BranchController {

    private final BranchBasicService branchBasicService;

    @Operation(
        summary = "Get repository branches",
        description = "Returns all branches of a repository with their last commit information"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Branches retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Repository not found")
    })
    @GetMapping("/{repoId}")
    public ResponseEntity<List<BranchResponse>> getRepositoryBranches(
            @Parameter(description = "Repository ID", required = true)
            @PathVariable UUID repoId) {

        List<BranchResponse> branches = branchBasicService.findByRepoId(repoId);
        return ResponseEntity.ok(branches);
    }

    @Operation(
        summary = "Get branch details",
        description = "Returns detailed information about a branch including recent commits (last 10) and statistics"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Branch found"),
        @ApiResponse(responseCode = "404", description = "Branch not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<BranchDetailResponse> getBranchDetails(
            @Parameter(description = "Branch ID", required = true)
            @PathVariable UUID branchId) {

        return branchBasicService.findByIdWithCommits(branchId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

