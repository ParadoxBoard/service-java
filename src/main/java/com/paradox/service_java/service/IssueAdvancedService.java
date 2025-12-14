package com.paradox.service_java.service;

import com.paradox.service_java.dto.IssueLabelGroupResponse;
import com.paradox.service_java.dto.IssueSimpleResponse;
import com.paradox.service_java.model.GithubIssue;
import com.paradox.service_java.repository.GithubIssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio avanzado para issues de GitHub
 * Responsabilidad: DEV B (Isabella)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IssueAdvancedService {

    private final GithubIssueRepository githubIssueRepository;

    /**
     * Obtener issues agrupados por labels
     */
    public List<IssueLabelGroupResponse> getIssuesByLabels(UUID repoId) {
        log.info("Getting issues grouped by labels for repo: {}", repoId);

        List<GithubIssue> allIssues = githubIssueRepository.findByRepoId(repoId);

        // Extraer todos los labels únicos
        Set<String> allLabels = allIssues.stream()
                .filter(issue -> issue.getLabels() != null)
                .flatMap(issue -> issue.getLabels().stream())
                .collect(Collectors.toSet());

        // Agrupar issues por cada label
        return allLabels.stream()
                .map(label -> {
                    List<GithubIssue> issuesWithLabel = githubIssueRepository.findByRepoIdAndLabel(repoId, label);

                    long openCount = issuesWithLabel.stream()
                            .filter(issue -> "open".equals(issue.getState()))
                            .count();

                    long closedCount = issuesWithLabel.stream()
                            .filter(issue -> "closed".equals(issue.getState()))
                            .count();

                    List<IssueSimpleResponse> issueResponses = issuesWithLabel.stream()
                            .map(this::mapToSimpleResponse)
                            .collect(Collectors.toList());

                    return IssueLabelGroupResponse.builder()
                            .label(label)
                            .totalIssues(issuesWithLabel.size())
                            .openIssues((int) openCount)
                            .closedIssues((int) closedCount)
                            .issues(issueResponses)
                            .build();
                })
                .sorted(Comparator.comparingInt(IssueLabelGroupResponse::getTotalIssues).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Obtener issues asignados a un usuario específico
     */
    public List<IssueSimpleResponse> getIssuesAssignedToUser(String username) {
        log.info("Getting issues assigned to user: {}", username);

        List<GithubIssue> issues = githubIssueRepository.findByAssigneesContaining(username);

        return issues.stream()
                .map(this::mapToSimpleResponse)
                .sorted(Comparator.comparing(IssueSimpleResponse::getUpdatedAt).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Mapear GithubIssue a IssueSimpleResponse
     */
    private IssueSimpleResponse mapToSimpleResponse(GithubIssue issue) {
        return IssueSimpleResponse.builder()
                .id(issue.getId())
                .number(issue.getNumber())
                .title(issue.getTitle())
                .state(issue.getState())
                .repoName(issue.getRepo().getName())
                .labels(issue.getLabels() != null ? issue.getLabels() : List.of())
                .assignees(issue.getAssignees() != null ? issue.getAssignees() : List.of())
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                .htmlUrl(issue.getHtmlUrl())
                .build();
    }
}

