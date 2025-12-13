package com.paradox.service_java.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * DTO para eventos de Pull Request desde GitHub Webhooks
 * Creado por DEV B (Isabella)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PullRequestEventDTO {

    private String action;

    @JsonProperty("number")
    private Integer number;

    @JsonProperty("pull_request")
    private PullRequestData pullRequest;

    @JsonProperty("repository")
    private RepositoryData repository;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PullRequestData {
        private Long id;

        @JsonProperty("node_id")
        private String nodeId;

        private Integer number;
        private String state;
        private String title;
        private String body;

        @JsonProperty("html_url")
        private String htmlUrl;

        private Boolean draft;
        private Boolean merged;
        private Boolean mergeable;

        @JsonProperty("merged_by")
        private UserData mergedBy;

        @JsonProperty("merged_at")
        private OffsetDateTime mergedAt;

        @JsonProperty("closed_at")
        private OffsetDateTime closedAt;

        @JsonProperty("created_at")
        private OffsetDateTime createdAt;

        @JsonProperty("updated_at")
        private OffsetDateTime updatedAt;

        private UserData user;

        private BranchData head;
        private BranchData base;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserData {
        private Long id;
        private String login;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BranchData {
        private String ref;
        private String sha;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RepositoryData {
        private Long id;

        @JsonProperty("full_name")
        private String fullName;
    }
}

