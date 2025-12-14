package com.paradox.service_java.service;

import com.paradox.service_java.dto.PaginatedResponse;
import com.paradox.service_java.dto.PullRequestResponse;
import com.paradox.service_java.model.PullRequest;
import com.paradox.service_java.repository.PullRequestRepository;
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
 * Servicio para Pull Requests
 * DEV A (Adrian)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PullRequestService {

    private final PullRequestRepository pullRequestRepository;

    @Transactional(readOnly = true)
    public PaginatedResponse<PullRequestResponse> findByRepoWithFilters(
            UUID repoId, String state, String author, int page, int size) {

        log.info("Finding PRs - repo: {}, state: {}, author: {}, page: {}, size: {}",
                repoId, state, author, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<PullRequest> prPage;

        if (state != null && !state.isEmpty() && author != null && !author.isEmpty()) {
            prPage = pullRequestRepository.findByRepoIdAndStateAndUserLogin(repoId, state, author, pageable);
        } else if (state != null && !state.isEmpty()) {
            prPage = pullRequestRepository.findByRepoIdAndState(repoId, state, pageable);
        } else if (author != null && !author.isEmpty()) {
            prPage = pullRequestRepository.findByRepoIdAndUserLogin(repoId, author, pageable);
        } else {
            prPage = pullRequestRepository.findByRepoId(repoId, pageable);
        }

        List<PullRequestResponse> content = prPage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PaginatedResponse.of(content, page, size, prPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Optional<PullRequestResponse> findByNumberAndRepo(Integer number, UUID repoId) {
        log.info("Finding PR #{} in repo: {}", number, repoId);

        return pullRequestRepository.findByRepoIdAndNumber(repoId, number)
                .map(this::toResponse);
    }

    private PullRequestResponse toResponse(PullRequest pr) {
        return PullRequestResponse.builder()
                .id(pr.getId())
                .githubPrId(pr.getGithubPrId())
                .number(pr.getNumber())
                .nodeId(pr.getNodeId())
                .state(pr.getState())
                .title(pr.getTitle())
                .body(pr.getBody())
                .userLogin(pr.getUserLogin())
                .userId(pr.getUserId())
                .headRef(pr.getHeadRef())
                .headSha(pr.getHeadSha())
                .baseRef(pr.getBaseRef())
                .baseSha(pr.getBaseSha())
                .draft(pr.getDraft())
                .merged(pr.getMerged())
                .mergeable(pr.getMergeable())
                .mergeableState(null) // Field not in model yet
                .mergedAt(pr.getMergedAt())
                .mergedBy(pr.getMergedBy())
                .additions(null) // Field not in model yet
                .deletions(null) // Field not in model yet
                .changedFiles(null) // Field not in model yet
                .commitsCount(null) // Field not in model yet
                .htmlUrl(pr.getHtmlUrl())
                .createdAt(pr.getCreatedAt())
                .updatedAt(pr.getUpdatedAt())
                .repositoryId(pr.getRepo().getId())
                .repositoryName(pr.getRepo().getName())
                .repositoryFullName(pr.getRepo().getFullName())
                .build();
    }
}

