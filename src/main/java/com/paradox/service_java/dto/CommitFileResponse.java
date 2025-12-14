package com.paradox.service_java.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para archivos modificados en un commit
 * Responsabilidad: DEV B (Isabella)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommitFileResponse {
    private String filename;
    private String status; // added, modified, removed, renamed
    private Integer additions;
    private Integer deletions;
    private Integer changes;
    private String blobUrl;
    private String rawUrl;
    private String patch;
}

