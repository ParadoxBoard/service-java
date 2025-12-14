package com.paradox.service_java.service;
}
    }
                .build();
                .issuesOpen((int) openIssues)
                .pullRequestsOpen((int) openPullRequests)
                .commitsLast30Days((int) commitsLast30Days)
                .reposByLanguage(reposByLanguage)
                .activeRepos((int) activeRepos)
                .archivedRepos((int) archivedRepos)
                .privateRepos((int) privateRepos)
                .publicRepos((int) publicRepos)
                .totalRepos((int) totalRepos)
        return RepoStatsResponse.builder()

                .count();
                .filter(issue -> "open".equals(issue.getState()))
        long openIssues = githubIssueRepository.findAll().stream()
        // Issues abiertos

                .count();
                .filter(pr -> "open".equals(pr.getState()))
        long openPullRequests = pullRequestRepository.findAll().stream()
        // PRs abiertos

        long commitsLast30Days = commitRepository.countCommitsSince(thirtyDaysAgo);
        OffsetDateTime thirtyDaysAgo = OffsetDateTime.now().minusDays(30);
        // Commits de los últimos 30 días

                ));
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                        com.paradox.service_java.model.Repository::getLanguage,
                .collect(Collectors.groupingBy(
                .filter(repo -> repo.getLanguage() != null && !repo.getLanguage().isEmpty())
        Map<String, Integer> reposByLanguage = allRepos.stream()
        // Agrupar por lenguaje

        long activeRepos = totalRepos - archivedRepos;
        long archivedRepos = repositoryRepository.countByArchived(true);
        long privateRepos = repositoryRepository.countByPrivateRepo(true);
        long publicRepos = repositoryRepository.countByPrivateRepo(false);
        long totalRepos = allRepos.size();
        // Contar por tipo

        List<com.paradox.service_java.model.Repository> allRepos = repositoryRepository.findAll();
        // Obtener todos los repos

        log.info("Calculating general repository statistics");
    public RepoStatsResponse getGeneralStats() {
     */
     * Obtener estadísticas generales de todos los repositorios
    /**

    private final GithubIssueRepository githubIssueRepository;
    private final PullRequestRepository pullRequestRepository;
    private final CommitRepository commitRepository;
    private final RepositoryRepository repositoryRepository;

public class RepoStatsService {
@RequiredArgsConstructor
@Service
@Slf4j
 */
 * Responsabilidad: DEV B (Isabella)
 * Servicio para estadísticas de repositorios
/**

import java.util.stream.Collectors;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.paradox.service_java.repository.*;
import com.paradox.service_java.dto.RepoStatsResponse;


