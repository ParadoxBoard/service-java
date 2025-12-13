package com.paradox.service_java.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad para branches de repositorios
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "branches",
       uniqueConstraints = @UniqueConstraint(name = "branches_repo_name_key", columnNames = {"repo_id", "name"}))
public class Branch {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repo_id", nullable = false)
    private Repository repository;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "sha", nullable = false)
    private String sha;

    @Column(name = "protected")
    private Boolean protectedBranch;

    @Column(name = "commit_message", columnDefinition = "text")
    private String commitMessage;

    @Column(name = "commit_author")
    private String commitAuthor;

    @Column(name = "commit_date")
    private OffsetDateTime commitDate;

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
        if (protectedBranch == null) {
            protectedBranch = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}

