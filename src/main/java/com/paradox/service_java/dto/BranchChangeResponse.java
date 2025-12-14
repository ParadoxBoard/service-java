package com.paradox.service_java.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO para cambios recientes en branches
 * Responsabilidad: DEV B (Isabella)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchChangeResponse {
    private UUID branchId;
    private String branchName;
    private String repoName;
    private UUID repoId;
    private String lastCommitSha;
    private String lastCommitMessage;
    private String lastCommitAuthor;
    private OffsetDateTime lastCommitDate;
    private Integer openPullRequests;
    private Boolean hasRecentActivity; // Actividad en últimas 24h
}

