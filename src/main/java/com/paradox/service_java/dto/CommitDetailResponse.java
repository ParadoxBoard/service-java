package com.paradox.service_java.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO para detalles completos de commit
 * DEV A (Adrian)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommitDetailResponse {

    private UUID id;
    private String sha;
    private String message;
    private String treeSha;
    private String authorName;
    private String authorEmail;
    private String authorLogin;
    private OffsetDateTime authorDate;
    private Integer additions;
    private Integer deletions;
    private Integer changedFiles;
    private String htmlUrl;
    private OffsetDateTime createdAt;

    // Parent commits (detalles completos)
    private List<String> parents;

    // Branch info
    private UUID branchId;
    private String branchName;

    // Repository info
    private UUID repositoryId;
    private String repositoryName;
    private String repositoryFullName;

    // Files changed (placeholder - se puede agregar después)
    // private List<FileChange> filesChanged;
}

