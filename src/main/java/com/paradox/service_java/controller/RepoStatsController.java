package com.paradox.service_java.controller;

import com.paradox.service_java.dto.RepoStatsResponse;
import com.paradox.service_java.service.RepoStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para estadísticas de repositorios
 * Responsabilidad: DEV B (Isabella)
 */
@RestController
@RequestMapping("/api/repos")
@Tag(name = "Repository Stats", description = "Repository statistics and metrics operations")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class RepoStatsController {

    private final RepoStatsService repoStatsService;

    @Operation(
        summary = "Get repository statistics",
        description = "Returns general statistics about all repositories: total, public/private, by language, recent activity"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/stats")
    public ResponseEntity<RepoStatsResponse> getRepoStats() {
        RepoStatsResponse stats = repoStatsService.getGeneralStats();
        return ResponseEntity.ok(stats);
    }
}

