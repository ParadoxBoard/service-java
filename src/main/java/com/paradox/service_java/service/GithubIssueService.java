package com.paradox.service_java.service;

import com.paradox.service_java.dto.GithubIssueResponse;
import com.paradox.service_java.dto.PaginatedResponse;
import com.paradox.service_java.model.GithubIssue;
import com.paradox.service_java.repository.GithubIssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio para GitHub Issues
 * DEV A (Adrian)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GithubIssueService {

    private final GithubIssueRepository githubIssueRepository;

    @Transactional(readOnly = true)
    public PaginatedResponse<GithubIssueResponse> findByRepoWithFilters(
            UUID repoId, String state, int page, int size) {

        log.info("Finding issues - repo: {}, state: {}, page: {}, size: {}", repoId, state, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<GithubIssue> issuePage;

        if (state != null && !state.isEmpty()) {
            issuePage = githubIssueRepository.findByRepoIdAndState(repoId, state, pageable);
        } else {
            issuePage = githubIssueRepository.findByRepoId(repoId, pageable);
        }

        List<GithubIssueResponse> content = issuePage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PaginatedResponse.of(content, page, size, issuePage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Optional<GithubIssueResponse> findByNumberAndRepo(Integer number, UUID repoId) {
        log.info("Finding issue #{} in repo: {}", number, repoId);

        return githubIssueRepository.findByRepoIdAndNumber(repoId, number)
                .map(this::toResponse);
    }

    private GithubIssueResponse toResponse(GithubIssue issue) {
        return GithubIssueResponse.builder()
                .id(issue.getId())
                .githubIssueId(issue.getGithubIssueId())
                .number(issue.getNumber())
                .nodeId(issue.getNodeId())
                .state(issue.getState())
                .title(issue.getTitle())
                .body(issue.getBody())
                .userLogin(issue.getUserLogin())
                .userId(issue.getUserId())
                .labels(issue.getLabels())
                .assignees(issue.getAssignees())
                .milestone(issue.getMilestone())
                .locked(issue.getLocked())
                .commentsCount(issue.getCommentsCount())
                .closedAt(issue.getClosedAt())
                .htmlUrl(issue.getHtmlUrl())
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                .repositoryId(issue.getRepo().getId())
                .repositoryName(issue.getRepo().getName())
                .repositoryFullName(issue.getRepo().getFullName())
                .build();
    }
}

