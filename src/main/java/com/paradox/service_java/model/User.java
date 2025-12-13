package com.paradox.service_java.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users",
       uniqueConstraints = {
           @UniqueConstraint(name = "users_email_key", columnNames = "email"),
           @UniqueConstraint(name = "users_username_key", columnNames = "username")
       })
public class User {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "username", unique = true, length = 255)
    private String username;

    // SQL column name is "name"
    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "github_id", length = 255)
    private String githubId;

    @Column(name = "github_installation_id", length = 255)
    private String githubInstallationId;

    // DB: created_at timestamptz DEFAULT now() NULL
    @Column(name = "created_at", columnDefinition = "timestamptz default now()")
    private OffsetDateTime createdAt;

    @Column(name = "last_seen_at", columnDefinition = "timestamptz")
    private OffsetDateTime lastSeenAt;
}
