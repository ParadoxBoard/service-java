package com.paradox.service_java.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO para respuesta de issue de GitHub
 * DEV A (Adrian)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GithubIssueResponse {

    private UUID id;
    private Long githubIssueId;
    private Integer number;
    private String nodeId;
    private String state;
    private String title;
    private String body;
    private String userLogin;
    private Long userId;
    private List<String> labels;
    private List<String> assignees;
    private String milestone;
    private Boolean locked;
    private Integer commentsCount;
    private OffsetDateTime closedAt;
    private String htmlUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Repository info
    private UUID repositoryId;
    private String repositoryName;
    private String repositoryFullName;
}

