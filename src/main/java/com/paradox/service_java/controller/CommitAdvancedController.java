package com.paradox.service_java.controller;

import com.paradox.service_java.dto.CommitFileResponse;
import com.paradox.service_java.model.Commit;
import com.paradox.service_java.service.CommitAdvancedService;
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
 * Controlador avanzado para commits
 * Responsabilidad: DEV B (Isabella)
 */
@RestController
@RequestMapping("/api/commits")
@Tag(name = "Commits (Advanced)", description = "Advanced commit endpoints (DEV B)")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class CommitAdvancedController {

    private final CommitAdvancedService commitAdvancedService;

    @Operation(
        summary = "Get commits by branch name",
        description = "Returns commits from a specific branch by name, with pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Commits retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Repository or branch not found")
    })
    @GetMapping("/{branchName}")
    public ResponseEntity<List<Commit>> getCommitsByBranchName(
            @Parameter(description = "Branch name", required = true)
            @PathVariable String branchName,
            @Parameter(description = "Repository ID", required = true)
            @RequestParam UUID repoId,
            @Parameter(description = "Maximum number of commits to return", required = false)
            @RequestParam(defaultValue = "50") int limit) {
        List<Commit> commits = commitAdvancedService.getCommitsByBranchName(repoId, branchName, limit);
        return ResponseEntity.ok(commits);
    }

    @Operation(
        summary = "Get files modified in a commit",
        description = "Returns the list of files modified in a specific commit with additions/deletions stats from GitHub API"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Files retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Commit not found")
    })
    @GetMapping("/{sha}/files")
    public ResponseEntity<List<CommitFileResponse>> getCommitFiles(
            @Parameter(description = "Commit SHA", required = true)
            @PathVariable String sha,
            @Parameter(description = "GitHub Installation ID", required = true)
            @RequestParam Long installationId,
            @Parameter(description = "Repository owner", required = true)
            @RequestParam String owner,
            @Parameter(description = "Repository name", required = true)
            @RequestParam String repoName) {
        List<CommitFileResponse> files = commitAdvancedService.getCommitFiles(sha, installationId, owner, repoName);
        return ResponseEntity.ok(files);
    }
}

