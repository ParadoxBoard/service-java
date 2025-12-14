package com.paradox.service_java.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO para respuesta de branch (resumen)
 * DEV A (Adrian)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchResponse {

    private UUID id;
    private String name;
    private String headSha;
    private String headMessage;
    private String headAuthor;
    private OffsetDateTime headDate;
    private Boolean isProtected;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // Repository info
    private UUID repositoryId;
    private String repositoryName;
}

