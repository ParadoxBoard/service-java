package com.paradox.service_java.controller;

import com.paradox.service_java.dto.IssueLabelGroupResponse;
import com.paradox.service_java.dto.IssueSimpleResponse;
import com.paradox.service_java.service.IssueAdvancedService;
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
 * Controlador avanzado para GitHub Issues
 * Responsabilidad: DEV B (Isabella)
 */
@RestController
@RequestMapping("/api/github/issues")
@Tag(name = "GitHub Issues (Advanced)", description = "Advanced GitHub issues endpoints (DEV B)")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class IssueAdvancedController {

    private final IssueAdvancedService issueAdvancedService;

    @Operation(
        summary = "Get issues grouped by labels",
        description = "Returns issues grouped by their labels with counts for open/closed"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Issues retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Repository not found")
    })
    @GetMapping("/labels")
    public ResponseEntity<List<IssueLabelGroupResponse>> getIssuesByLabels(
            @Parameter(description = "Repository ID", required = true)
            @RequestParam UUID repoId) {
        List<IssueLabelGroupResponse> groups = issueAdvancedService.getIssuesByLabels(repoId);
        return ResponseEntity.ok(groups);
    }

    @Operation(
        summary = "Get issues assigned to a user",
        description = "Returns all issues assigned to a specific GitHub username"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Issues retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/assigned")
    public ResponseEntity<List<IssueSimpleResponse>> getIssuesAssignedToUser(
            @Parameter(description = "GitHub username", required = true)
            @RequestParam String username) {
        List<IssueSimpleResponse> issues = issueAdvancedService.getIssuesAssignedToUser(username);
        return ResponseEntity.ok(issues);
    }
}

