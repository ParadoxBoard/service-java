package com.paradox.service_java.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO simplificado para issues
 * Responsabilidad: DEV B (Isabella)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueSimpleResponse {
    private UUID id;
    private Integer number;
    private String title;
    private String state;
    private String repoName;
    private List<String> labels;
    private List<String> assignees;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String htmlUrl;
}

