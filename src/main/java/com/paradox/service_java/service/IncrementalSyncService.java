package com.paradox.service_java.service;

import com.paradox.service_java.model.GithubIssue;
import com.paradox.service_java.model.Installation;
import com.paradox.service_java.model.PullRequest;
import com.paradox.service_java.model.Repository;
import com.paradox.service_java.repository.GithubIssueRepository;
import com.paradox.service_java.repository.InstallationRepository;
import com.paradox.service_java.repository.PullRequestRepository;
import com.paradox.service_java.repository.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Servicio de Sincronización Incremental
 * Responsabilidad: DEV B (Isabella)
 *
 * Detecta y sincroniza solo los cambios desde la última sincronización
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IncrementalSyncService {

    private final GitHubApiService gitHubApiService;
    private final InstallationRepository installationRepository;
    private final RepositoryRepository repositoryRepository;
    private final PullRequestRepository pullRequestRepository;
    private final GithubIssueRepository githubIssueRepository;
    private final InstallationTokenService installationTokenService;

    /**
     * Sincroniza todos los cambios desde GitHub para una instalación
     */
    @Transactional
    public SyncSummary syncFull(Long githubInstallationId) {
        log.info("Starting full incremental sync for installation: {}", githubInstallationId);

        SyncSummary summary = new SyncSummary();
        summary.setStartTime(OffsetDateTime.now());
        summary.setInstallationId(githubInstallationId);

        try {
            // Buscar instalación
            Installation installation = installationRepository.findByInstallationId(githubInstallationId)
                    .orElseThrow(() -> new RuntimeException("Installation not found: " + githubInstallationId));

            // Obtener token de instalación
            String token = installationTokenService.getInstallationToken(githubInstallationId);

            // Obtener repositorios de la instalación
            List<Repository> repositories = repositoryRepository.findByInstallationId(installation.getId());
            log.info("Found {} repositories for installation {}", repositories.size(), githubInstallationId);

            // Sincronizar cada repositorio
            for (Repository repo : repositories) {
                try {
                    syncRepository(repo, token, summary);
                } catch (Exception e) {
                    log.error("Error syncing repository {}: {}", repo.getFullName(), e.getMessage(), e);
                    summary.addError(repo.getFullName(), e.getMessage());
                }
            }

            summary.setEndTime(OffsetDateTime.now());
            summary.setSuccess(true);
            log.info("Full sync completed for installation {}: {}", githubInstallationId, summary);

        } catch (Exception e) {
            log.error("Error in full sync for installation {}: {}", githubInstallationId, e.getMessage(), e);
            summary.setSuccess(false);
            summary.setEndTime(OffsetDateTime.now());
            summary.addError("GLOBAL", e.getMessage());
        }

        return summary;
    }

    /**
     * Sincroniza cambios de un repositorio específico
     */
    @Transactional
    public void syncRepository(Repository repo, String token, SyncSummary summary) {
        log.info("Syncing repository: {}", repo.getFullName());

        // Sincronizar PRs
        syncPullRequests(repo, token, summary);

        // Sincronizar Issues
        syncIssues(repo, token, summary);

        // TODO DEV A: Sincronizar Commits (requiere entidad Commit)
        // TODO DEV A: Sincronizar Branches (requiere entidad Branch)
    }

    /**
     * Sincroniza Pull Requests modificados desde última sincronización
     */
    private void syncPullRequests(Repository repo, String token, SyncSummary summary) {
        try {
            log.info("Syncing Pull Requests for repo: {}", repo.getFullName());

            // Obtener fecha de última actualización
            OffsetDateTime lastUpdate = repo.getUpdatedAt();

            // Obtener PRs desde GitHub (todos los estados)
            List<Map<String, Object>> openPrs = gitHubApiService.getPullRequests(
                    repo.getFullName(), "open", token);
            List<Map<String, Object>> closedPrs = gitHubApiService.getPullRequests(
                    repo.getFullName(), "closed", token);

            int syncedCount = 0;
            int updatedCount = 0;
            int newCount = 0;

            // Procesar PRs abiertos
            for (Map<String, Object> prData : openPrs) {
                if (syncPullRequest(repo, prData)) {
                    syncedCount++;
                    if (isNew(repo, (Integer) prData.get("number"))) {
                        newCount++;
                    } else {
                        updatedCount++;
                    }
                }
            }

            // Procesar PRs cerrados (solo los modificados recientemente)
            for (Map<String, Object> prData : closedPrs) {
                String updatedAtStr = (String) prData.get("updated_at");
                if (updatedAtStr != null) {
                    OffsetDateTime updatedAt = OffsetDateTime.parse(updatedAtStr);
                    if (lastUpdate == null || updatedAt.isAfter(lastUpdate)) {
                        if (syncPullRequest(repo, prData)) {
                            syncedCount++;
                            updatedCount++;
                        }
                    }
                }
            }

            summary.addPullRequestsSynced(syncedCount);
            summary.addPullRequestsCreated(newCount);
            summary.addPullRequestsUpdated(updatedCount);

            log.info("Synced {} PRs for repo {} ({} new, {} updated)",
                    syncedCount, repo.getFullName(), newCount, updatedCount);

        } catch (Exception e) {
            log.error("Error syncing PRs for repo {}: {}", repo.getFullName(), e.getMessage(), e);
            summary.addError(repo.getFullName() + " (PRs)", e.getMessage());
        }
    }

    /**
     * Sincroniza Issues modificados desde última sincronización
     */
    private void syncIssues(Repository repo, String token, SyncSummary summary) {
        try {
            log.info("Syncing Issues for repo: {}", repo.getFullName());

            // Obtener fecha de última actualización
            OffsetDateTime lastUpdate = repo.getUpdatedAt();

            // Obtener issues desde GitHub (todos los estados)
            List<Map<String, Object>> openIssues = gitHubApiService.getIssues(
                    repo.getFullName(), "open", token);
            List<Map<String, Object>> closedIssues = gitHubApiService.getIssues(
                    repo.getFullName(), "closed", token);

            int syncedCount = 0;
            int updatedCount = 0;
            int newCount = 0;

            // Procesar issues abiertos
            for (Map<String, Object> issueData : openIssues) {
                // Filtrar PRs (GitHub API devuelve PRs en issues)
                if (issueData.containsKey("pull_request")) {
                    continue;
                }

                if (syncIssue(repo, issueData)) {
                    syncedCount++;
                    if (isNewIssue(repo, (Integer) issueData.get("number"))) {
                        newCount++;
                    } else {
                        updatedCount++;
                    }
                }
            }

            // Procesar issues cerrados (solo los modificados recientemente)
            for (Map<String, Object> issueData : closedIssues) {
                if (issueData.containsKey("pull_request")) {
                    continue;
                }

                String updatedAtStr = (String) issueData.get("updated_at");
                if (updatedAtStr != null) {
                    OffsetDateTime updatedAt = OffsetDateTime.parse(updatedAtStr);
                    if (lastUpdate == null || updatedAt.isAfter(lastUpdate)) {
                        if (syncIssue(repo, issueData)) {
                            syncedCount++;
                            updatedCount++;
                        }
                    }
                }
            }

            summary.addIssuesSynced(syncedCount);
            summary.addIssuesCreated(newCount);
            summary.addIssuesUpdated(updatedCount);

            log.info("Synced {} issues for repo {} ({} new, {} updated)",
                    syncedCount, repo.getFullName(), newCount, updatedCount);

        } catch (Exception e) {
            log.error("Error syncing issues for repo {}: {}", repo.getFullName(), e.getMessage(), e);
            summary.addError(repo.getFullName() + " (Issues)", e.getMessage());
        }
    }

    /**
     * Sincroniza un Pull Request individual
     */
    private boolean syncPullRequest(Repository repo, Map<String, Object> prData) {
        try {
            Integer number = (Integer) prData.get("number");
            Long githubPrId = ((Number) prData.get("id")).longValue();
            String state = (String) prData.get("state");
            String title = (String) prData.get("title");
            String body = (String) prData.get("body");

            // Buscar o crear PR
            PullRequest pr = pullRequestRepository.findByRepoIdAndNumber(repo.getId(), number)
                    .orElse(PullRequest.builder()
                            .repo(repo)
                            .number(number)
                            .build());

            // Actualizar datos
            pr.setGithubPrId(githubPrId);
            pr.setState(state);
            pr.setTitle(title);
            pr.setBody(body);

            // Extraer datos adicionales
            if (prData.containsKey("user") && prData.get("user") instanceof Map) {
                Map<String, Object> user = (Map<String, Object>) prData.get("user");
                pr.setUserLogin((String) user.get("login"));
                pr.setUserId(((Number) user.get("id")).longValue());
            }

            if (prData.containsKey("head") && prData.get("head") instanceof Map) {
                Map<String, Object> head = (Map<String, Object>) prData.get("head");
                pr.setHeadRef((String) head.get("ref"));
                pr.setHeadSha((String) head.get("sha"));
            }

            if (prData.containsKey("base") && prData.get("base") instanceof Map) {
                Map<String, Object> base = (Map<String, Object>) prData.get("base");
                pr.setBaseRef((String) base.get("ref"));
                pr.setBaseSha((String) base.get("sha"));
            }

            pr.setDraft((Boolean) prData.get("draft"));
            pr.setMerged((Boolean) prData.get("merged"));
            pr.setHtmlUrl((String) prData.get("html_url"));

            pullRequestRepository.save(pr);
            return true;

        } catch (Exception e) {
            log.error("Error syncing PR: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Sincroniza un Issue individual
     */
    private boolean syncIssue(Repository repo, Map<String, Object> issueData) {
        try {
            Integer number = (Integer) issueData.get("number");
            Long githubIssueId = ((Number) issueData.get("id")).longValue();
            String state = (String) issueData.get("state");
            String title = (String) issueData.get("title");
            String body = (String) issueData.get("body");

            // Buscar o crear issue
            GithubIssue issue = githubIssueRepository.findByRepoIdAndNumber(repo.getId(), number)
                    .orElse(GithubIssue.builder()
                            .repo(repo)
                            .number(number)
                            .build());

            // Actualizar datos
            issue.setGithubIssueId(githubIssueId);
            issue.setState(state);
            issue.setTitle(title);
            issue.setBody(body);

            // Extraer usuario
            if (issueData.containsKey("user") && issueData.get("user") instanceof Map) {
                Map<String, Object> user = (Map<String, Object>) issueData.get("user");
                issue.setUserLogin((String) user.get("login"));
                issue.setUserId(((Number) user.get("id")).longValue());
            }

            // Extraer labels
            if (issueData.containsKey("labels") && issueData.get("labels") instanceof List) {
                List<Map<String, Object>> labels = (List<Map<String, Object>>) issueData.get("labels");
                List<String> labelNames = new ArrayList<>();
                for (Map<String, Object> label : labels) {
                    labelNames.add((String) label.get("name"));
                }
                issue.setLabels(labelNames);
            }

            // Extraer assignees
            if (issueData.containsKey("assignees") && issueData.get("assignees") instanceof List) {
                List<Map<String, Object>> assignees = (List<Map<String, Object>>) issueData.get("assignees");
                List<String> assigneeLogins = new ArrayList<>();
                for (Map<String, Object> assignee : assignees) {
                    assigneeLogins.add((String) assignee.get("login"));
                }
                issue.setAssignees(assigneeLogins);
            }

            issue.setCommentsCount((Integer) issueData.get("comments"));
            issue.setHtmlUrl((String) issueData.get("html_url"));

            githubIssueRepository.save(issue);
            return true;

        } catch (Exception e) {
            log.error("Error syncing issue: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Verifica si un PR es nuevo
     */
    private boolean isNew(Repository repo, Integer number) {
        return pullRequestRepository.findByRepoIdAndNumber(repo.getId(), number).isEmpty();
    }

    /**
     * Verifica si un Issue es nuevo
     */
    private boolean isNewIssue(Repository repo, Integer number) {
        return githubIssueRepository.findByRepoIdAndNumber(repo.getId(), number).isEmpty();
    }

    /**
     * Clase para resumir los resultados de la sincronización
     */
    public static class SyncSummary {
        private Long installationId;
        private OffsetDateTime startTime;
        private OffsetDateTime endTime;
        private boolean success;

        private int pullRequestsSynced = 0;
        private int pullRequestsCreated = 0;
        private int pullRequestsUpdated = 0;

        private int issuesSynced = 0;
        private int issuesCreated = 0;
        private int issuesUpdated = 0;

        private int commitsSynced = 0; // TODO DEV A
        private int branchesSynced = 0; // TODO DEV A

        private Map<String, String> errors = new HashMap<>();

        // Getters y Setters
        public Long getInstallationId() { return installationId; }
        public void setInstallationId(Long installationId) { this.installationId = installationId; }

        public OffsetDateTime getStartTime() { return startTime; }
        public void setStartTime(OffsetDateTime startTime) { this.startTime = startTime; }

        public OffsetDateTime getEndTime() { return endTime; }
        public void setEndTime(OffsetDateTime endTime) { this.endTime = endTime; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public int getPullRequestsSynced() { return pullRequestsSynced; }
        public void addPullRequestsSynced(int count) { this.pullRequestsSynced += count; }

        public int getPullRequestsCreated() { return pullRequestsCreated; }
        public void addPullRequestsCreated(int count) { this.pullRequestsCreated += count; }

        public int getPullRequestsUpdated() { return pullRequestsUpdated; }
        public void addPullRequestsUpdated(int count) { this.pullRequestsUpdated += count; }

        public int getIssuesSynced() { return issuesSynced; }
        public void addIssuesSynced(int count) { this.issuesSynced += count; }

        public int getIssuesCreated() { return issuesCreated; }
        public void addIssuesCreated(int count) { this.issuesCreated += count; }

        public int getIssuesUpdated() { return issuesUpdated; }
        public void addIssuesUpdated(int count) { this.issuesUpdated += count; }

        public int getCommitsSynced() { return commitsSynced; }
        public void addCommitsSynced(int count) { this.commitsSynced += count; }

        public int getBranchesSynced() { return branchesSynced; }
        public void addBranchesSynced(int count) { this.branchesSynced += count; }

        public Map<String, String> getErrors() { return errors; }
        public void addError(String context, String message) { this.errors.put(context, message); }

        @Override
        public String toString() {
            return String.format("SyncSummary{installation=%d, PRs=%d(%d new, %d updated), Issues=%d(%d new, %d updated), errors=%d}",
                    installationId, pullRequestsSynced, pullRequestsCreated, pullRequestsUpdated,
                    issuesSynced, issuesCreated, issuesUpdated, errors.size());
        }
    }
}

