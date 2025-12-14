package com.paradox.service_java.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de respuesta para instalaciones de GitHub App
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallationResponse {

    private UUID id;

    private Long installationId; // GitHub installation ID

    private String accountType; // "User" o "Organization"

    private String accountLogin; // "adr1ann32323" o "ParadoxBoard"

    private Integer repositoryCount; // Número de repos vinculados

    private Boolean active; // Si está activa o fue desinstalada

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}

