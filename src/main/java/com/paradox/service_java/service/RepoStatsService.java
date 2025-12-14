package com.paradox.service_java.service;

import com.paradox.service_java.dto.RepoStatsResponse;
import com.paradox.service_java.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio para estadísticas de repositorios
 * Responsabilidad: DEV B (Isabella)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RepoStatsService {

    private final RepositoryRepository repositoryRepository;
    private final CommitRepository commitRepository;
    private final PullRequestRepository pullRequestRepository;
    private final GithubIssueRepository githubIssueRepository;

    /**
     * Obtener estadísticas generales de todos los repositorios
     */
    public RepoStatsResponse getGeneralStats() {
        log.info("Calculating general repository statistics");

        // Obtener todos los repos
        List<com.paradox.service_java.model.Repository> allRepos = repositoryRepository.findAll();

        // Contar por tipo
        long totalRepos = allRepos.size();
        long publicRepos = repositoryRepository.countByPrivateRepo(false);
        long privateRepos = repositoryRepository.countByPrivateRepo(true);
        long archivedRepos = repositoryRepository.countByArchived(true);
        long activeRepos = totalRepos - archivedRepos;

        // Agrupar por lenguaje
        Map<String, Integer> reposByLanguage = allRepos.stream()
                .filter(repo -> repo.getLanguage() != null && !repo.getLanguage().isEmpty())
                .collect(Collectors.groupingBy(
                        com.paradox.service_java.model.Repository::getLanguage,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        // Commits de los últimos 30 días
        OffsetDateTime thirtyDaysAgo = OffsetDateTime.now().minusDays(30);
        long commitsLast30Days = commitRepository.countCommitsSince(thirtyDaysAgo);

        // PRs abiertos
        long openPullRequests = pullRequestRepository.findAll().stream()
                .filter(pr -> "open".equals(pr.getState()))
                .count();

        // Issues abiertos
        long openIssues = githubIssueRepository.findAll().stream()
                .filter(issue -> "open".equals(issue.getState()))
                .count();

        return RepoStatsResponse.builder()
                .totalRepos((int) totalRepos)
                .publicRepos((int) publicRepos)
                .privateRepos((int) privateRepos)
                .archivedRepos((int) archivedRepos)
                .activeRepos((int) activeRepos)
                .reposByLanguage(reposByLanguage)
                .commitsLast30Days((int) commitsLast30Days)
                .pullRequestsOpen((int) openPullRequests)
                .issuesOpen((int) openIssues)
                .build();
    }
}

