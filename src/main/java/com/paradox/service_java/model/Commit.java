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
 * Entidad para commits de GitHub
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "commits",
       uniqueConstraints = @UniqueConstraint(name = "commits_repo_sha_key", columnNames = {"repo_id", "sha"}))
public class Commit {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repo_id", nullable = false)
    private Repository repository;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "sha", nullable = false)
    private String sha;

    @Column(name = "node_id")
    private String nodeId;

    @Column(name = "message", nullable = false, columnDefinition = "text")
    private String message;

    @Column(name = "author_name")
    private String authorName;

    @Column(name = "author_email")
    private String authorEmail;

    @Column(name = "author_login")
    private String authorLogin;

    @Column(name = "author_date")
    private OffsetDateTime authorDate;

    @Column(name = "committer_name")
    private String committerName;

    @Column(name = "committer_email")
    private String committerEmail;

    @Column(name = "committer_date")
    private OffsetDateTime committerDate;

    @Column(name = "tree_sha")
    private String treeSha;

    @Column(name = "parent_shas", columnDefinition = "text[]")
    private List<String> parentShas;

    @Column(name = "additions")
    private Integer additions;

    @Column(name = "deletions")
    private Integer deletions;

    @Column(name = "changed_files")
    private Integer changedFiles;

    @Column(name = "html_url")
    private String htmlUrl;

    @Column(name = "verified")
    private Boolean verified;

    @Column(name = "created_at", columnDefinition = "timestamptz default now()")
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (verified == null) {
            verified = false;
        }
    }
}

