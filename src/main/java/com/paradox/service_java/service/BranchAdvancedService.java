package com.paradox.service_java.service;

import com.paradox.service_java.dto.BranchChangeResponse;
import com.paradox.service_java.dto.BranchProtectionResponse;
import com.paradox.service_java.model.Branch;
import com.paradox.service_java.model.PullRequest;
import com.paradox.service_java.repository.BranchRepository;
import com.paradox.service_java.repository.PullRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio avanzado para branches
 * Responsabilidad: DEV B (Isabella)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BranchAdvancedService {

    private final BranchRepository branchRepository;
    private final PullRequestRepository pullRequestRepository;
    private final GitHubApiService gitHubApiService;
    private final InstallationTokenService installationTokenService;

    /**
     * Obtener branches con cambios recientes (últimas 24 horas)
     */
    public List<BranchChangeResponse> getRecentChanges(UUID repoId) {
        log.info("Getting recent branch changes for repo: {}", repoId);

        OffsetDateTime twentyFourHoursAgo = OffsetDateTime.now().minusHours(24);
        List<Branch> recentBranches = branchRepository.findRecentlyUpdatedBranches(repoId, twentyFourHoursAgo);

        return recentBranches.stream()
                .map(branch -> {
                    // Contar PRs abiertos para este branch
                    long openPRs = pullRequestRepository.findByRepoId(repoId).stream()
                            .filter(pr -> "open".equals(pr.getState()))
                            .filter(pr -> branch.getName().equals(pr.getHeadRef()))
                            .count();

                    return BranchChangeResponse.builder()
                            .branchId(branch.getId())
                            .branchName(branch.getName())
                            .repoName(branch.getRepository().getName())
                            .repoId(repoId)
                            .lastCommitSha(branch.getSha())
                            .lastCommitMessage(branch.getCommitMessage())
                            .lastCommitAuthor(branch.getCommitAuthor())
                            .lastCommitDate(branch.getCommitDate())
                            .openPullRequests((int) openPRs)
                            .hasRecentActivity(true)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Obtener configuración de protección de un branch desde GitHub API
     */
    public BranchProtectionResponse getBranchProtection(UUID branchId, Long installationId) {
        log.info("Getting branch protection for branch: {}", branchId);

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found: " + branchId));

        // Si no está protegido según nuestra BD, retornar respuesta simple
        if (!Boolean.TRUE.equals(branch.getProtectedBranch())) {
            return BranchProtectionResponse.builder()
                    .branchName(branch.getName())
                    .isProtected(false)
                    .requiresReview(false)
                    .requiredReviewers(0)
                    .requiresStatusChecks(false)
                    .requiresUpToDateBranch(false)
                    .restrictsPushes(false)
                    .allowsForcePushes(true)
                    .allowsDeletions(true)
                    .build();
        }

        // Si está protegido, intentar obtener detalles de GitHub API
        try {
            String token = installationTokenService.getInstallationToken(installationId);
            String owner = branch.getRepository().getOwnerLogin();
            String repo = branch.getRepository().getName();
            String branchName = branch.getName();

            Map<String, Object> protectionData = gitHubApiService.getBranchProtection(
                    owner, repo, branchName, token
            );

            return mapProtectionData(branchName, protectionData);

        } catch (Exception e) {
            log.warn("Could not fetch branch protection from GitHub API: {}", e.getMessage());
            // Retornar datos básicos de nuestra BD
            return BranchProtectionResponse.builder()
                    .branchName(branch.getName())
                    .isProtected(true)
                    .requiresReview(null)
                    .requiredReviewers(null)
                    .requiresStatusChecks(null)
                    .requiresUpToDateBranch(null)
                    .restrictsPushes(null)
                    .allowsForcePushes(null)
                    .allowsDeletions(null)
                    .build();
        }
    }

    private BranchProtectionResponse mapProtectionData(String branchName, Map<String, Object> data) {
        BranchProtectionResponse.BranchProtectionResponseBuilder builder = BranchProtectionResponse.builder()
                .branchName(branchName)
                .isProtected(true);

        if (data.containsKey("required_pull_request_reviews")) {
            Map<String, Object> reviewsData = (Map<String, Object>) data.get("required_pull_request_reviews");
            builder.requiresReview(true);
            builder.requiredReviewers(
                    reviewsData.containsKey("required_approving_review_count")
                            ? (Integer) reviewsData.get("required_approving_review_count")
                            : 1
            );
        } else {
            builder.requiresReview(false);
            builder.requiredReviewers(0);
        }

        if (data.containsKey("required_status_checks")) {
            Map<String, Object> statusData = (Map<String, Object>) data.get("required_status_checks");
            builder.requiresStatusChecks(true);
            builder.requiresUpToDateBranch(
                    statusData.containsKey("strict") ? (Boolean) statusData.get("strict") : false
            );
        } else {
            builder.requiresStatusChecks(false);
            builder.requiresUpToDateBranch(false);
        }

        builder.restrictsPushes(data.containsKey("restrictions"));
        builder.allowsForcePushes(
                data.containsKey("allow_force_pushes")
                        ? (Boolean) ((Map<String, Object>) data.get("allow_force_pushes")).get("enabled")
                        : false
        );
        builder.allowsDeletions(
                data.containsKey("allow_deletions")
                        ? (Boolean) ((Map<String, Object>) data.get("allow_deletions")).get("enabled")
                        : false
        );

        return builder.build();
    }
}

