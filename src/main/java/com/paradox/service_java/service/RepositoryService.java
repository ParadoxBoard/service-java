package com.paradox.service_java.service;

import com.paradox.service_java.dto.RepositoryDetailResponse;
import com.paradox.service_java.dto.RepositoryResponse;
import com.paradox.service_java.model.Installation;
import com.paradox.service_java.model.Repository;
import com.paradox.service_java.model.User;
import com.paradox.service_java.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar repositorios
 * DEV A (Adrian)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryService {

    private final RepositoryRepository repositoryRepository;
    private final BranchRepository branchRepository;
    private final CommitRepository commitRepository;
    private final GithubIssueRepository githubIssueRepository;
    private final PullRequestRepository pullRequestRepository;
    private final UserRepository userRepository;

    /**
     * Obtiene todos los repositorios de un usuario por su email
     */
    @Transactional(readOnly = true)
    public List<RepositoryResponse> findAllByUserEmail(String userEmail) {
        log.info("Finding repositories for user: {}", userEmail);

        Optional<User> userOpt = userRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            log.warn("User not found: {}", userEmail);
            return List.of();
        }

        User user = userOpt.get();

        // Buscar por installation si existe
        if (user.getGithubInstallationId() != null) {
            Long installationId = Long.parseLong(user.getGithubInstallationId());
            List<Repository> repos = repositoryRepository.findByInstallation_InstallationId(installationId);
            return repos.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        log.warn("User has no github installation: {}", userEmail);
        return List.of();
    }

    /**
     * Obtiene detalles de un repositorio por ID con estadísticas
     */
    @Transactional(readOnly = true)
    public Optional<RepositoryDetailResponse> findByIdWithStats(UUID repoId) {
        log.info("Finding repository details for id: {}", repoId);

        Optional<Repository> repoOpt = repositoryRepository.findById(repoId);
        if (repoOpt.isEmpty()) {
            return Optional.empty();
        }

        Repository repo = repoOpt.get();

        // Calcular estadísticas
        Long branchesCount = branchRepository.countByRepository_Id(repoId);
        Long commitsCount = commitRepository.countByRepository_Id(repoId);
        Long openIssuesCount = githubIssueRepository.countByRepoIdAndState(repoId, "open");
        Long openPrsCount = pullRequestRepository.countByRepoIdAndState(repoId, "open");

        // Obtener último commit
        var lastCommit = commitRepository.findTopByRepository_IdOrderByAuthorDateDesc(repoId);

        return Optional.of(toDetailResponse(repo, branchesCount, commitsCount,
                openIssuesCount, openPrsCount, lastCommit.orElse(null)));
    }

    /**
     * Convierte Repository a RepositoryResponse
     */
    private RepositoryResponse toResponse(Repository repo) {
        return RepositoryResponse.builder()
                .id(repo.getId())
                .githubRepoId(repo.getGithubRepoId())
                .name(repo.getName())
                .fullName(repo.getFullName())
                .ownerLogin(repo.getOwnerLogin())
                .privateRepo(repo.getPrivateRepo())
                .description(repo.getDescription())
                .htmlUrl(repo.getHtmlUrl())
                .defaultBranch(repo.getDefaultBranch())
                .language(repo.getLanguage())
                .topics(repo.getTopics())
                .archived(repo.getArchived())
                .fork(repo.getFork())
                .pushedAt(repo.getPushedAt())
                .createdAt(repo.getCreatedAt())
                .updatedAt(repo.getUpdatedAt())
                .build();
    }

    /**
     * Convierte Repository a RepositoryDetailResponse con stats
     */
    private RepositoryDetailResponse toDetailResponse(Repository repo, Long branchesCount,
            Long commitsCount, Long openIssuesCount, Long openPrsCount,
            com.paradox.service_java.model.Commit lastCommit) {

        return RepositoryDetailResponse.builder()
                .id(repo.getId())
                .githubRepoId(repo.getGithubRepoId())
                .name(repo.getName())
                .fullName(repo.getFullName())
                .ownerLogin(repo.getOwnerLogin())
                .privateRepo(repo.getPrivateRepo())
                .description(repo.getDescription())
                .htmlUrl(repo.getHtmlUrl())
                .cloneUrl(repo.getCloneUrl())
                .defaultBranch(repo.getDefaultBranch())
                .language(repo.getLanguage())
                .topics(repo.getTopics())
                .archived(repo.getArchived())
                .fork(repo.getFork())
                .pushedAt(repo.getPushedAt())
                .createdAt(repo.getCreatedAt())
                .updatedAt(repo.getUpdatedAt())
                .branchesCount(branchesCount)
                .commitsCount(commitsCount)
                .openIssuesCount(openIssuesCount)
                .openPrsCount(openPrsCount)
                .lastCommitSha(lastCommit != null ? lastCommit.getSha() : null)
                .lastCommitDate(lastCommit != null ? lastCommit.getAuthorDate() : null)
                .build();
    }
}

