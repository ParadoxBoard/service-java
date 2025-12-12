package com.paradox.service_java.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Entidad para almacenar Issues de GitHub
 * (Diferente de los issues/tareas internos del sistema)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "github_issues",
       uniqueConstraints = @UniqueConstraint(name = "github_issues_repo_id_number_key", columnNames = {"repo_id", "number"}))
public class GithubIssue {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repo_id", nullable = false)
    private Repository repo;

    @Column(name = "github_issue_id", nullable = false)
    private Long githubIssueId;

    @Column(name = "number", nullable = false)
    private Integer number;

    @Column(name = "node_id")
    private String nodeId;

    @Column(name = "state")
    private String state; // open, closed

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "body", columnDefinition = "text")
    private String body;

    @Column(name = "user_login")
    private String userLogin;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "labels", columnDefinition = "text[]")
    private List<String> labels;

    @Column(name = "assignees", columnDefinition = "text[]")
    private List<String> assignees;

    @Column(name = "milestone")
    private String milestone;

    @Column(name = "locked")
    private Boolean locked;

    @Column(name = "comments_count")
    private Integer commentsCount;

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

