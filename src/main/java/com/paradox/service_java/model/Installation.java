package com.paradox.service_java.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Entidad para almacenar instalaciones de GitHub App
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "installations")
public class Installation {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "installation_id", nullable = false, unique = true)
    private Long installationId;

    @Column(name = "account_login", nullable = false)
    private String accountLogin;

    @Column(name = "account_type", nullable = false)
    private String accountType; // 'Organization' o 'User'

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "target_type")
    private String targetType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "permissions", columnDefinition = "jsonb")
    private Map<String, Object> permissions;

    @Column(name = "events", columnDefinition = "text[]")
    private List<String> events;

    @Column(name = "repository_selection")
    private String repositorySelection; // 'all' o 'selected'

    @Column(name = "app_id")
    private Long appId;

    @Column(name = "app_slug")
    private String appSlug;

    @Column(name = "suspended_at")
    private OffsetDateTime suspendedAt;

    @Column(name = "created_at", columnDefinition = "timestamptz default now()")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "timestamptz default now()")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}

