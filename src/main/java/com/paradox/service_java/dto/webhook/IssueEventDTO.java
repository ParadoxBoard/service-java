package com.paradox.service_java.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO para eventos de Issues desde GitHub Webhooks
 * Creado por DEV B (Isabella)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IssueEventDTO {

    private String action; // opened, closed, reopened, edited, labeled, etc.

    @JsonProperty("issue")
    private IssueData issue;

    @JsonProperty("repository")
    private RepositoryData repository;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IssueData {
        private Long id;

        @JsonProperty("node_id")
        private String nodeId;

        private Integer number;
        private String state;
        private String title;
        private String body;

        @JsonProperty("html_url")
        private String htmlUrl;

        private Boolean locked;

        @JsonProperty("comments")
        private Integer comments;

        @JsonProperty("closed_at")
        private OffsetDateTime closedAt;

        @JsonProperty("created_at")
        private OffsetDateTime createdAt;

        @JsonProperty("updated_at")
        private OffsetDateTime updatedAt;

        private UserData user;

        private List<LabelData> labels;

        private List<UserData> assignees;

        private MilestoneData milestone;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserData {
        private Long id;
        private String login;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LabelData {
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MilestoneData {
        private String title;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RepositoryData {
        private Long id;

        @JsonProperty("full_name")
        private String fullName;
    }
}

