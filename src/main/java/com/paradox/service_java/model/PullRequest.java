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
 * Entidad para almacenar Pull Requests de GitHub
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pull_requests",
       uniqueConstraints = @UniqueConstraint(name = "pull_requests_repo_id_number_key", columnNames = {"repo_id", "number"}))
public class PullRequest {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repo_id", nullable = false)
    private Repository repo;

    @Column(name = "github_pr_id", nullable = false)
    private Long githubPrId;

    @Column(name = "number", nullable = false)
    private Integer number;

    @Column(name = "node_id")
    private String nodeId;

    @Column(name = "state")
    private String state; // open, closed, merged

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "body", columnDefinition = "text")
    private String body;

    @Column(name = "user_login")
    private String userLogin;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "head_ref")
    private String headRef;

    @Column(name = "head_sha")
    private String headSha;

    @Column(name = "base_ref")
    private String baseRef;

    @Column(name = "base_sha")
    private String baseSha;

    @Column(name = "draft")
    private Boolean draft;

    @Column(name = "merged")
    private Boolean merged;

    @Column(name = "mergeable")
    private Boolean mergeable;

    @Column(name = "merged_by")
    private String mergedBy;

    @Column(name = "merged_at")
    private OffsetDateTime mergedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "html_url")
    private String htmlUrl;

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

