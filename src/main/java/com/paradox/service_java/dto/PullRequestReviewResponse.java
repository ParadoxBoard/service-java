package com.paradox.service_java.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO para reviews de pull requests
 * Responsabilidad: DEV B (Isabella)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PullRequestReviewResponse {
    private Long id;
    private String reviewer;
    private String state; // APPROVED, CHANGES_REQUESTED, COMMENTED, DISMISSED
    private String body;
    private Integer commentsCount;
    private OffsetDateTime submittedAt;
    private String htmlUrl;
}

