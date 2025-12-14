package com.paradox.service_java.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO simplificado para pull requests
 * Responsabilidad: DEV B (Isabella)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PullRequestSimpleResponse {
    private UUID id;
    private Integer number;
    private String title;
    private String state;
    private String repoName;
    private String author;
    private Boolean draft;
    private Boolean merged;
    private Boolean mergeable;
    private String headRef;
    private String baseRef;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String htmlUrl;
}

