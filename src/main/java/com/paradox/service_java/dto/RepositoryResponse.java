package com.paradox.service_java.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO para respuesta de repositorio (resumen)
 * DEV A (Adrian)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryResponse {

    private UUID id;
    private Long githubRepoId;
    private String name;
    private String fullName;
    private String ownerLogin;
    private Boolean privateRepo;
    private String description;
    private String htmlUrl;
    private String defaultBranch;
    private String language;
    private List<String> topics;
    private Boolean archived;
    private Boolean fork;
    private OffsetDateTime pushedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

