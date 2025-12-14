package com.paradox.service_java.service;

import com.paradox.service_java.dto.PullRequestReviewResponse;
import com.paradox.service_java.dto.PullRequestSimpleResponse;
import com.paradox.service_java.model.PullRequest;
import com.paradox.service_java.repository.PullRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio avanzado para Pull Requests
 * Responsabilidad: DEV B (Isabella)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PullRequestAdvancedService {

    private final PullRequestRepository pullRequestRepository;
    private final GitHubApiService gitHubApiService;
    private final InstallationTokenService installationTokenService;

    /**
     * Obtener solo PRs abiertos de un repositorio
     */
    public List<PullRequestSimpleResponse> getOpenPullRequests(UUID repoId) {
        log.info("Getting open pull requests for repo: {}", repoId);

        List<PullRequest> openPRs = pullRequestRepository.findOpenPRsByRepoId(repoId);

        return openPRs.stream()
                .map(this::mapToSimpleResponse)
                .sorted(Comparator.comparing(PullRequestSimpleResponse::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Obtener reviews de un Pull Request desde GitHub API
     */
    public List<PullRequestReviewResponse> getPullRequestReviews(UUID repoId, Integer prNumber, Long installationId) {
        log.info("Getting reviews for PR #{} in repo: {}", prNumber, repoId);

        PullRequest pr = pullRequestRepository.findByRepoIdAndNumber(repoId, prNumber)
                .orElseThrow(() -> new RuntimeException("Pull Request not found: " + prNumber));

        try {
            String token = installationTokenService.getInstallationToken(installationId);
            String owner = pr.getRepo().getOwnerLogin();
            String repoName = pr.getRepo().getName();

            List<Map<String, Object>> reviews = gitHubApiService.getPullRequestReviews(
                    owner, repoName, prNumber, token
            );

            return reviews.stream()
                    .map(review -> PullRequestReviewResponse.builder()
                            .id(review.containsKey("id") ? ((Number) review.get("id")).longValue() : null)
                            .reviewer(review.containsKey("user") && review.get("user") != null
                                    ? (String) ((Map<String, Object>) review.get("user")).get("login")
                                    : "unknown")
                            .state((String) review.get("state"))
                            .body((String) review.get("body"))
                            .commentsCount(0) // GitHub API no devuelve esto directamente
                            .submittedAt(review.containsKey("submitted_at") && review.get("submitted_at") != null
                                    ? OffsetDateTime.parse((String) review.get("submitted_at"))
                                    : null)
                            .htmlUrl((String) review.get("html_url"))
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching PR reviews from GitHub API: {}", e.getMessage());
            throw new RuntimeException("Could not fetch PR reviews: " + e.getMessage());
        }
    }

    /**
     * Mapear PullRequest a PullRequestSimpleResponse
     */
    private PullRequestSimpleResponse mapToSimpleResponse(PullRequest pr) {
        return PullRequestSimpleResponse.builder()
                .id(pr.getId())
                .number(pr.getNumber())
                .title(pr.getTitle())
                .state(pr.getState())
                .repoName(pr.getRepo().getName())
                .author(pr.getUserLogin())
                .draft(pr.getDraft())
                .merged(pr.getMerged())
                .mergeable(pr.getMergeable())
                .headRef(pr.getHeadRef())
                .baseRef(pr.getBaseRef())
                .createdAt(pr.getCreatedAt())
                .updatedAt(pr.getUpdatedAt())
                .htmlUrl(pr.getHtmlUrl())
                .build();
    }
}

