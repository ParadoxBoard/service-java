package com.paradox.service_java.controller;

import com.paradox.service_java.model.Repository;
import com.paradox.service_java.repository.RepositoryRepository;
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
 * Controlador para repos asociados a proyectos
 * Responsabilidad: DEV B (Isabella)
 *
 * NOTA: Este es un placeholder. Cuando se implemente el módulo de proyectos interno,
 * se deberá crear la entidad Project y la relación con Repository.
 * Por ahora, retorna repos por installationId como simulación.
 */
@RestController
@RequestMapping("/api/repos")
@Tag(name = "Project Repositories", description = "Repository and project association operations")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class ProjectRepoController {

    private final RepositoryRepository repositoryRepository;

    @Operation(
        summary = "Get repositories by project (placeholder)",
        description = "Returns repositories associated with a project. Currently returns repos by installationId. " +
                      "Will be updated when the Project entity is implemented."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Repositories retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Project not found")
    })
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Repository>> getReposByProject(
            @Parameter(description = "Project ID (currently using installationId as placeholder)", required = true)
            @PathVariable UUID projectId) {

        // TODO: Cuando exista la entidad Project, hacer:
        // Project project = projectRepository.findById(projectId)...
        // return project.getRepositories();

        // Por ahora, retornamos repos por installationId
        List<Repository> repos = repositoryRepository.findByInstallationId(projectId);

        return ResponseEntity.ok(repos);
    }
}

