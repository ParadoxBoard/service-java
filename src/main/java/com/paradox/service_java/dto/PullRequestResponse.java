package com.paradox.service_java.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO para respuesta de Pull Request
 * DEV A (Adrian)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PullRequestResponse {

    private UUID id;
    private Long githubPrId;
    private Integer number;
    private String nodeId;
    private String state;
    private String title;
    private String body;
    private String userLogin;
    private Long userId;
    private String headRef;
    private String headSha;
    private String baseRef;
    private String baseSha;
    private Boolean draft;
    private Boolean merged;
    private Boolean mergeable;
    private String mergeableState;
    private OffsetDateTime mergedAt;
    private String mergedBy;
    private Integer additions;
    private Integer deletions;
    private Integer changedFiles;
    private Integer commitsCount;
    private String htmlUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Repository info
    private UUID repositoryId;
    private String repositoryName;
    private String repositoryFullName;
}

