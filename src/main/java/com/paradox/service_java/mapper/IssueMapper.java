package com.paradox.service_java.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.paradox.service_java.dto.webhook.IssueEventDTO;
import com.paradox.service_java.model.GithubIssue;
import com.paradox.service_java.model.Repository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para convertir eventos de Issues de GitHub a entidades
 * Creado por DEV B (Isabella)
 */
@Component
public class IssueMapper {

    /**
     * Convierte un JsonNode de webhook a entidad GithubIssue
     */
    public GithubIssue fromJsonNode(JsonNode json, Repository repository) {
        JsonNode issueNode = json.path("issue");

        // Extraer labels
        List<String> labels = new ArrayList<>();
        JsonNode labelsNode = issueNode.path("labels");
        if (labelsNode.isArray()) {
            for (JsonNode labelNode : labelsNode) {
                labels.add(labelNode.path("name").asText());
            }
        }

        // Extraer assignees
        List<String> assignees = new ArrayList<>();
        JsonNode assigneesNode = issueNode.path("assignees");
        if (assigneesNode.isArray()) {
            for (JsonNode assigneeNode : assigneesNode) {
                assignees.add(assigneeNode.path("login").asText());
            }
        }

        // Extraer milestone
        String milestone = null;
        JsonNode milestoneNode = issueNode.path("milestone");
        if (!milestoneNode.isMissingNode() && !milestoneNode.isNull()) {
            milestone = milestoneNode.path("title").asText(null);
        }

        return GithubIssue.builder()
                .repo(repository)
                .githubIssueId(issueNode.path("id").asLong())
                .number(issueNode.path("number").asInt())
                .nodeId(issueNode.path("node_id").asText(null))
                .state(issueNode.path("state").asText(null))
                .title(issueNode.path("title").asText())
                .body(issueNode.path("body").asText(null))
                .userLogin(issueNode.path("user").path("login").asText(null))
                .userId(issueNode.path("user").path("id").asLong(0L))
                .labels(labels.isEmpty() ? null : labels)
                .assignees(assignees.isEmpty() ? null : assignees)
                .milestone(milestone)
                .locked(issueNode.path("locked").asBoolean(false))
                .commentsCount(issueNode.path("comments").asInt(0))
                .closedAt(parseDateTime(issueNode.path("closed_at").asText(null)))
                .htmlUrl(issueNode.path("html_url").asText(null))
                .createdAt(parseDateTime(issueNode.path("created_at").asText(null)))
                .updatedAt(parseDateTime(issueNode.path("updated_at").asText(null)))
                .build();
    }

    /**
     * Actualiza una entidad GithubIssue existente con datos del DTO
     */
    public void updateEntity(GithubIssue entity, IssueEventDTO dto) {
        IssueEventDTO.IssueData issueData = dto.getIssue();

        if (issueData == null) {
            return;
        }

        entity.setGithubIssueId(issueData.getId());
        entity.setNodeId(issueData.getNodeId());
        entity.setState(issueData.getState());
        entity.setTitle(issueData.getTitle());
        entity.setBody(issueData.getBody());
        entity.setHtmlUrl(issueData.getHtmlUrl());
        entity.setLocked(issueData.getLocked());
        entity.setCommentsCount(issueData.getComments());
        entity.setClosedAt(issueData.getClosedAt());

        if (issueData.getUser() != null) {
            entity.setUserLogin(issueData.getUser().getLogin());
            entity.setUserId(issueData.getUser().getId());
        }

        if (issueData.getLabels() != null) {
            List<String> labels = issueData.getLabels().stream()
                    .map(IssueEventDTO.LabelData::getName)
                    .collect(Collectors.toList());
            entity.setLabels(labels.isEmpty() ? null : labels);
        }

        if (issueData.getAssignees() != null) {
            List<String> assignees = issueData.getAssignees().stream()
                    .map(IssueEventDTO.UserData::getLogin)
                    .collect(Collectors.toList());
            entity.setAssignees(assignees.isEmpty() ? null : assignees);
        }

        if (issueData.getMilestone() != null) {
            entity.setMilestone(issueData.getMilestone().getTitle());
        }
    }

    /**
     * Parse datetime string to OffsetDateTime
     */
    private OffsetDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty() || "null".equals(dateTimeStr)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }
}

