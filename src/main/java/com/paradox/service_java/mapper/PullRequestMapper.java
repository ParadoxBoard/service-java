package com.paradox.service_java.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.paradox.service_java.dto.webhook.PullRequestEventDTO;
import com.paradox.service_java.model.PullRequest;
import com.paradox.service_java.model.Repository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Mapper para convertir eventos de Pull Request de GitHub a entidades
 * Creado por DEV B (Isabella)
 */
@Component
public class PullRequestMapper {

    /**
     * Convierte un JsonNode de webhook a entidad PullRequest
     */
    public PullRequest fromJsonNode(JsonNode json, Repository repository) {
        JsonNode prNode = json.path("pull_request");

        return PullRequest.builder()
                .repo(repository)
                .githubPrId(prNode.path("id").asLong())
                .number(prNode.path("number").asInt())
                .nodeId(prNode.path("node_id").asText(null))
                .state(prNode.path("state").asText(null))
                .title(prNode.path("title").asText())
                .body(prNode.path("body").asText(null))
                .userLogin(prNode.path("user").path("login").asText(null))
                .userId(prNode.path("user").path("id").asLong(0L))
                .headRef(prNode.path("head").path("ref").asText(null))
                .headSha(prNode.path("head").path("sha").asText(null))
                .baseRef(prNode.path("base").path("ref").asText(null))
                .baseSha(prNode.path("base").path("sha").asText(null))
                .draft(prNode.path("draft").asBoolean(false))
                .merged(prNode.path("merged").asBoolean(false))
                .mergeable(prNode.path("mergeable").isMissingNode() ? null : prNode.path("mergeable").asBoolean())
                .mergedBy(prNode.path("merged_by").path("login").asText(null))
                .mergedAt(parseDateTime(prNode.path("merged_at").asText(null)))
                .closedAt(parseDateTime(prNode.path("closed_at").asText(null)))
                .htmlUrl(prNode.path("html_url").asText(null))
                .createdAt(parseDateTime(prNode.path("created_at").asText(null)))
                .updatedAt(parseDateTime(prNode.path("updated_at").asText(null)))
                .build();
    }

    /**
     * Actualiza una entidad PullRequest existente con datos del DTO
     */
    public void updateEntity(PullRequest entity, PullRequestEventDTO dto) {
        PullRequestEventDTO.PullRequestData prData = dto.getPullRequest();

        if (prData == null) {
            return;
        }

        entity.setGithubPrId(prData.getId());
        entity.setNodeId(prData.getNodeId());
        entity.setState(prData.getState());
        entity.setTitle(prData.getTitle());
        entity.setBody(prData.getBody());
        entity.setHtmlUrl(prData.getHtmlUrl());
        entity.setDraft(prData.getDraft());
        entity.setMerged(prData.getMerged());
        entity.setMergeable(prData.getMergeable());
        entity.setMergedAt(prData.getMergedAt());
        entity.setClosedAt(prData.getClosedAt());

        if (prData.getUser() != null) {
            entity.setUserLogin(prData.getUser().getLogin());
            entity.setUserId(prData.getUser().getId());
        }

        if (prData.getMergedBy() != null) {
            entity.setMergedBy(prData.getMergedBy().getLogin());
        }

        if (prData.getHead() != null) {
            entity.setHeadRef(prData.getHead().getRef());
            entity.setHeadSha(prData.getHead().getSha());
        }

        if (prData.getBase() != null) {
            entity.setBaseRef(prData.getBase().getRef());
            entity.setBaseSha(prData.getBase().getSha());
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

