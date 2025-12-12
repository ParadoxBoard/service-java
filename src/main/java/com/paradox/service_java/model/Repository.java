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
 * Entidad para almacenar repositorios de GitHub
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "repositories",
       uniqueConstraints = @UniqueConstraint(name = "repositories_github_repo_id_key", columnNames = "github_repo_id"))
public class Repository {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installation_id", nullable = false)
    private Installation installation;

    @Column(name = "github_repo_id", nullable = false, unique = true)
    private Long githubRepoId;

    @Column(name = "node_id")
    private String nodeId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "owner_login", nullable = false)
    private String ownerLogin;

    @Column(name = "owner_type")
    private String ownerType;

    @Column(name = "private")
    private Boolean privateRepo;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "fork")
    private Boolean fork;

    @Column(name = "html_url")
    private String htmlUrl;

    @Column(name = "clone_url")
    private String cloneUrl;

    @Column(name = "ssh_url")
    private String sshUrl;

    @Column(name = "default_branch")
    private String defaultBranch;

    @Column(name = "language")
    private String language;

    @Column(name = "topics", columnDefinition = "text[]")
    private List<String> topics;

    @Column(name = "archived")
    private Boolean archived;

    @Column(name = "disabled")
    private Boolean disabled;

    @Column(name = "pushed_at")
    private OffsetDateTime pushedAt;

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

