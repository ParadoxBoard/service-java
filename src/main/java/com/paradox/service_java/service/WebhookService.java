package com.paradox.service_java.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paradox.service_java.model.Branch;
import com.paradox.service_java.dto.webhook.IssueEventDTO;
import com.paradox.service_java.dto.webhook.PullRequestEventDTO;
import com.paradox.service_java.mapper.IssueMapper;
import com.paradox.service_java.mapper.PullRequestMapper;
import com.paradox.service_java.model.GithubIssue;
import com.paradox.service_java.model.PullRequest;
import com.paradox.service_java.model.Repository;
import com.paradox.service_java.model.WebhookLog;
import com.paradox.service_java.repository.GithubIssueRepository;
import com.paradox.service_java.repository.PullRequestRepository;
import com.paradox.service_java.repository.RepositoryRepository;
import com.paradox.service_java.repository.WebhookLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service to handle GitHub webhook events
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final ObjectMapper objectMapper;
    private final WebhookLogRepository webhookLogRepository;
    private final InstallationService installationService;
    private final com.paradox.service_java.repository.InstallationRepository installationRepository;
    private final RepositoryRepository repositoryRepository;
    private final PullRequestRepository pullRequestRepository;
    private final GithubIssueRepository githubIssueRepository;
    private final BranchService branchService;
    private final CommitService commitService;
    private final CSharpNotificationService csharpNotificationService;

    // Mappers para conversión de DTOs (DEV B)
    private final PullRequestMapper pullRequestMapper;
    private final IssueMapper issueMapper;

    @Value("${webhook.secret:}")
    private String webhookSecret;

    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * Validates the GitHub webhook signature
     */
    public boolean validateSignature(String payload, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isEmpty()) {
            log.warn("Webhook secret not configured - skipping signature validation");
            return true; // Allow webhooks in dev if secret is not configured
        }

        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            log.error("Invalid signature header format");
            return false;
        }

        try {
            String expectedSignature = signatureHeader.substring(7); // Remove "sha256=" prefix
            String actualSignature = calculateHmacSHA256(payload, webhookSecret);

            boolean isValid = expectedSignature.equalsIgnoreCase(actualSignature);

            if (!isValid) {
                log.warn("Signature mismatch - Expected: {}, Actual: {}", expectedSignature, actualSignature);
            }

            return isValid;
        } catch (Exception e) {
            log.error("Error validating webhook signature: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Calculates HMAC SHA256 signature
     */
    private String calculateHmacSHA256(String data, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hmacBytes);
    }

    /**
     * Process webhook event based on type
     */
    @Transactional
    public void processWebhook(String eventType, String payload, String signature, String deliveryId) {
        WebhookLog webhookLog = null;

        try {
            // 1. Guardar log del webhook
            webhookLog = saveWebhookLog(eventType, payload, signature, deliveryId);

            // 2. Parsear JSON
            JsonNode json = objectMapper.readTree(payload);

            // 3. Procesar según el tipo de evento
            switch (eventType) {
                case "installation" -> handleInstallationEvent(json);
                case "installation_repositories" -> handleInstallationRepositoriesEvent(json);
                case "push" -> handlePushEvent(json);
                case "create" -> handleCreateEvent(json);
                case "delete" -> handleDeleteEvent(json);
                case "pull_request" -> handlePullRequestEvent(json);
                case "issues" -> handleIssuesEvent(json);
                case "ping" -> handlePingEvent(json);
                default -> log.info("Unhandled webhook event type: {}", eventType);
            }

            // 4. Marcar como procesado
            if (webhookLog != null) {
                webhookLog.setProcessed(true);
                webhookLog.setProcessedAt(OffsetDateTime.now());
                webhookLogRepository.save(webhookLog);
            }

        } catch (Exception e) {
            log.error("Error processing webhook payload: {}", e.getMessage(), e);

            // Guardar error en el log
            if (webhookLog != null) {
                webhookLog.setProcessed(false);
                webhookLog.setErrorMessage(e.getMessage());
                webhookLogRepository.save(webhookLog);
            }

            throw new RuntimeException("Failed to process webhook", e);
        }
    }

    /**
     * Guarda el webhook en la base de datos
     */
    private WebhookLog saveWebhookLog(String eventType, String payload, String signature, String deliveryId) {
        try {
            // Convertir payload a Map
            Map<String, Object> payloadMap = objectMapper.readValue(
                payload,
                new TypeReference<Map<String, Object>>() {}
            );

            WebhookLog log = WebhookLog.builder()
                    .eventType(eventType)
                    .deliveryId(deliveryId)
                    .requestPayload(payloadMap)
                    .signature(signature)
                    .processed(false)
                    .build();

            return webhookLogRepository.save(log);
        } catch (Exception e) {
            log.error("Error saving webhook log: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save webhook log", e);
        }
    }

    /**
     * Handle installation events (created/deleted)
     */
    private void handleInstallationEvent(JsonNode json) {
        String action = json.path("action").asText();
        JsonNode installationNode = json.path("installation");

        long installationId = installationNode.path("id").asLong();
        String accountLogin = installationNode.path("account").path("login").asText();

        log.info("Installation event - Action: {}, InstallationId: {}, Account: {}",
                action, installationId, accountLogin);

        try {
            if ("created".equals(action)) {
                // Convertir JsonNode a Map
                Map<String, Object> installationData = objectMapper.convertValue(
                    installationNode,
                    new TypeReference<Map<String, Object>>() {}
                );

                // Crear o actualizar instalación en BD
                com.paradox.service_java.model.Installation installation =
                        installationService.createOrUpdateFromGitHub(installationData);
                log.info("Installation saved: {} for account: {}", installationId, accountLogin);

                // IMPORTANTE: Procesar repositorios que vienen en el payload de instalación
                JsonNode repositoriesNode = json.path("repositories");
                if (repositoriesNode.isArray() && repositoriesNode.size() > 0) {
                    log.info("Processing {} repositories from installation event", repositoriesNode.size());
                    int savedCount = 0;

                    for (JsonNode repoNode : repositoriesNode) {
                        try {
                            long githubRepoId = repoNode.path("id").asLong();
                            String nodeId = repoNode.path("node_id").asText();
                            String name = repoNode.path("name").asText();
                            String fullName = repoNode.path("full_name").asText();
                            boolean isPrivate = repoNode.path("private").asBoolean(false);

                            // Verificar si el repo ya existe
                            Optional<Repository> existingRepo = repositoryRepository.findByGithubRepoId(githubRepoId);

                            if (existingRepo.isEmpty()) {
                                // Crear nuevo repositorio
                                Repository newRepo = Repository.builder()
                                        .installation(installation)
                                        .githubRepoId(githubRepoId)
                                        .nodeId(nodeId)
                                        .name(name)
                                        .fullName(fullName)
                                        .ownerLogin(fullName.contains("/") ? fullName.split("/")[0] : accountLogin)
                                        .privateRepo(isPrivate)
                                        .createdAt(OffsetDateTime.now())
                                        .updatedAt(OffsetDateTime.now())
                                        .build();

                                repositoryRepository.save(newRepo);
                                savedCount++;
                                log.info("Repository saved from installation: {} ({})", fullName, githubRepoId);
                            } else {
                                log.debug("Repository already exists: {}, skipping", fullName);
                            }

                        } catch (Exception e) {
                            log.error("Error processing repository from installation: {}", e.getMessage(), e);
                        }
                    }

                    log.info("Saved {} repositories from installation event", savedCount);
                } else {
                    log.warn("No repositories found in installation event payload");
                }

            } else if ("deleted".equals(action)) {
                // Eliminar instalación
                installationService.delete(installationId);
                log.info("Installation deleted: {} for account: {}", installationId, accountLogin);

            } else if ("suspend".equals(action)) {
                // Marcar como suspendida
                installationService.suspend(installationId);
                log.info("Installation suspended: {} for account: {}", installationId, accountLogin);
            }
        } catch (Exception e) {
            log.error("Error handling installation event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to handle installation event", e);
        }
    }

    /**
     * Handle installation_repositories events (added/removed)
     * DEV A - Implementación completa
     */
    private void handleInstallationRepositoriesEvent(JsonNode json) {
        try {
            String action = json.path("action").asText();
            long installationId = json.path("installation").path("id").asLong();
            String repositorySelection = json.path("repository_selection").asText();

            log.info("Installation repositories event - Action: {}, InstallationId: {}, Selection: {}",
                    action, installationId, repositorySelection);

            if ("added".equals(action)) {
                // Procesar repositorios agregados
                JsonNode addedRepos = json.path("repositories_added");
                int addedCount = 0;

                // Buscar la instalación en BD
                Optional<com.paradox.service_java.model.Installation> installationOpt =
                        installationRepository.findByInstallationId(installationId);

                if (installationOpt.isEmpty()) {
                    log.warn("Installation not found in DB: {}, cannot add repositories", installationId);
                    return;
                }

                com.paradox.service_java.model.Installation installation = installationOpt.get();

                for (JsonNode repoNode : addedRepos) {
                    try {
                        long githubRepoId = repoNode.path("id").asLong();
                        String nodeId = repoNode.path("node_id").asText();
                        String name = repoNode.path("name").asText();
                        String fullName = repoNode.path("full_name").asText();
                        boolean isPrivate = repoNode.path("private").asBoolean();

                        // Verificar si el repo ya existe
                        Optional<Repository> existingRepo = repositoryRepository.findByGithubRepoId(githubRepoId);

                        if (existingRepo.isEmpty()) {
                            // Crear nuevo repositorio
                            Repository newRepo = Repository.builder()
                                    .installation(installation)
                                    .githubRepoId(githubRepoId)
                                    .nodeId(nodeId)
                                    .name(name)
                                    .fullName(fullName)
                                    .ownerLogin(fullName.split("/")[0])
                                    .privateRepo(isPrivate)
                                    .createdAt(OffsetDateTime.now())
                                    .updatedAt(OffsetDateTime.now())
                                    .build();

                            repositoryRepository.save(newRepo);
                            addedCount++;
                            log.info("Repository added to installation: {} ({})", fullName, githubRepoId);

                            // Trigger sincronización inicial del repo (opcional, puede ser costoso)
                            // installationService.syncSingleRepository(installationId, githubRepoId);
                        } else {
                            log.debug("Repository already exists: {}, skipping", fullName);
                        }

                    } catch (Exception e) {
                        log.error("Error processing added repository: {}", e.getMessage(), e);
                    }
                }

                log.info("Installation repositories added: {} new repos synchronized", addedCount);

            } else if ("removed".equals(action)) {
                // Procesar repositorios removidos
                JsonNode removedRepos = json.path("repositories_removed");
                int removedCount = 0;

                for (JsonNode repoNode : removedRepos) {
                    try {
                        long githubRepoId = repoNode.path("id").asLong();
                        String fullName = repoNode.path("full_name").asText();

                        // Buscar y eliminar (o marcar como inactivo) el repositorio
                        Optional<Repository> repoOpt = repositoryRepository.findByGithubRepoId(githubRepoId);

                        if (repoOpt.isPresent()) {
                            Repository repo = repoOpt.get();

                            // Opción A: Eliminar completamente (puede causar problemas de integridad)
                            // repositoryRepository.delete(repo);

                            // Opción B: Marcar como inactivo (recomendado)
                            repo.setInstallation(null); // Desasociar de la instalación
                            repo.setUpdatedAt(OffsetDateTime.now());
                            repositoryRepository.save(repo);

                            removedCount++;
                            log.info("Repository removed from installation: {} ({})", fullName, githubRepoId);
                        } else {
                            log.warn("Repository to remove not found in DB: {} ({})", fullName, githubRepoId);
                        }

                    } catch (Exception e) {
                        log.error("Error processing removed repository: {}", e.getMessage(), e);
                    }
                }

                log.info("Installation repositories removed: {} repos unlinked", removedCount);
            }

        } catch (Exception e) {
            log.error("Error handling installation_repositories event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to handle installation_repositories event", e);
        }
    }

    /**
     * Handle push events
     */
    private void handlePushEvent(JsonNode json) {
        try {
            String ref = json.path("ref").asText(); // refs/heads/main
            String repoFullName = json.path("repository").path("full_name").asText();
            Long githubRepoId = json.path("repository").path("id").asLong();
            JsonNode commits = json.path("commits");
            int commitCount = commits.size();

            log.info("Push event - Repo: {}, Ref: {}, Commits: {}", repoFullName, ref, commitCount);

            // Extraer nombre del branch del ref
            String branchName = ref.replace("refs/heads/", "");

            // 1. Buscar repositorio en BD
            Optional<Repository> repoOpt = repositoryRepository.findByGithubRepoId(githubRepoId);
            if (repoOpt.isEmpty()) {
                log.warn("Repository not found in DB: {} ({})", repoFullName, githubRepoId);
                return;
            }

            Repository repository = repoOpt.get();

            // 2. Obtener o crear branch
            JsonNode headCommitNode = json.path("head_commit");
            String headSha = headCommitNode.path("id").asText();
            String headMessage = headCommitNode.path("message").asText();
            String headAuthor = headCommitNode.path("author").path("name").asText();
            OffsetDateTime headDate = parseTimestamp(headCommitNode.path("timestamp").asText());

            Branch branch = branchService.createOrUpdate(
                    repository, branchName, headSha, headMessage, headAuthor, headDate
            );

            // 3. Procesar cada commit del push
            int savedCount = 0;
            for (JsonNode commitNode : commits) {
                try {
                    String sha = commitNode.path("id").asText();
                    String message = commitNode.path("message").asText();
                    String treeSha = commitNode.path("tree_id").asText();
                    String url = commitNode.path("url").asText();

                    // Author info
                    JsonNode authorNode = commitNode.path("author");
                    String authorName = authorNode.path("name").asText();
                    String authorEmail = authorNode.path("email").asText();
                    String authorLogin = authorNode.path("username").asText();
                    OffsetDateTime authorDate = parseTimestamp(commitNode.path("timestamp").asText());

                    // Stats (pueden no estar disponibles en push webhook)
                    Integer additions = commitNode.has("added") ? commitNode.path("added").size() : null;
                    Integer deletions = commitNode.has("removed") ? commitNode.path("removed").size() : null;
                    Integer modified = commitNode.has("modified") ? commitNode.path("modified").size() : null;

                    Integer changedFiles = 0;
                    if (additions != null) changedFiles += additions;
                    if (deletions != null) changedFiles += deletions;
                    if (modified != null) changedFiles += modified;

                    // Parent commits
                    List<String> parentShas = new ArrayList<>();
                    if (commitNode.has("parents")) {
                        commitNode.path("parents").forEach(parent ->
                                parentShas.add(parent.asText()));
                    }

                    // Crear commit si no existe
                    commitService.createIfNotExists(
                            repository, branch,
                            sha, message,
                            authorName, authorEmail, authorLogin,
                            authorDate,
                            treeSha, parentShas,
                            additions, deletions, changedFiles,
                            url
                    );

                    // Notificar a C# Service
                    csharpNotificationService.notifyCommitCreated(
                            repository.getId().toString(),
                            sha,
                            message,
                            authorLogin != null ? authorLogin : authorName
                    );

                    savedCount++;

                } catch (Exception e) {
                    log.error("Error processing individual commit: {}", e.getMessage(), e);
                }
            }

            log.info("Push event processed: {} commits saved/updated in branch {} of repo {}",
                    savedCount, branchName, repoFullName);

        } catch (Exception e) {
            log.error("Error handling push event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to handle push event", e);
        }
    }

    /**
     * Parse timestamp from GitHub ISO 8601 format
     */
    private OffsetDateTime parseTimestamp(String timestamp) {
        try {
            if (timestamp == null || timestamp.isEmpty()) {
                return OffsetDateTime.now();
            }
            return OffsetDateTime.parse(timestamp);
        } catch (Exception e) {
            log.warn("Failed to parse timestamp: {}, using current time", timestamp);
            return OffsetDateTime.now();
        }
    }

    /**
     * Handle create events (branch/tag creation)
     */
    private void handleCreateEvent(JsonNode json) {
        try {
            String refType = json.path("ref_type").asText(); // "branch" or "tag"
            String ref = json.path("ref").asText(); // branch/tag name
            String repoFullName = json.path("repository").path("full_name").asText();
            Long githubRepoId = json.path("repository").path("id").asLong();

            log.info("Create event - Repo: {}, Type: {}, Ref: {}", repoFullName, refType, ref);

            // Solo procesar si es un branch (ignorar tags por ahora)
            if (!"branch".equals(refType)) {
                log.debug("Ignoring create event for non-branch ref type: {}", refType);
                return;
            }

            // Buscar repositorio en BD
            Optional<Repository> repoOpt = repositoryRepository.findByGithubRepoId(githubRepoId);
            if (repoOpt.isEmpty()) {
                log.warn("Repository not found in DB: {} ({})", repoFullName, githubRepoId);
                return;
            }

            Repository repository = repoOpt.get();

            // Obtener info del sender (quien creó el branch)
            JsonNode senderNode = json.path("sender");
            String senderLogin = senderNode.path("login").asText();

            // Crear branch en BD
            String masterHeadSha = json.path("master_branch").asText();
            branchService.createOrUpdate(
                    repository,
                    ref,
                    masterHeadSha,
                    "Branch created via webhook",
                    senderLogin,
                    OffsetDateTime.now()
            );

            log.info("Branch created: {} in repo {}", ref, repoFullName);

            // Notificar a C# Service
            csharpNotificationService.notifyBranchCreated(
                    repository.getId().toString(),
                    ref,
                    masterHeadSha
            );

        } catch (Exception e) {
            log.error("Error handling create event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to handle create event", e);
        }
    }

    /**
     * Handle delete events (branch/tag deletion)
     */
    private void handleDeleteEvent(JsonNode json) {
        try {
            String refType = json.path("ref_type").asText(); // "branch" or "tag"
            String ref = json.path("ref").asText(); // branch/tag name
            String repoFullName = json.path("repository").path("full_name").asText();
            Long githubRepoId = json.path("repository").path("id").asLong();

            log.info("Delete event - Repo: {}, Type: {}, Ref: {}", repoFullName, refType, ref);

            // Solo procesar si es un branch (ignorar tags por ahora)
            if (!"branch".equals(refType)) {
                log.debug("Ignoring delete event for non-branch ref type: {}", refType);
                return;
            }

            // Buscar repositorio en BD
            Optional<Repository> repoOpt = repositoryRepository.findByGithubRepoId(githubRepoId);
            if (repoOpt.isEmpty()) {
                log.warn("Repository not found in DB: {} ({})", repoFullName, githubRepoId);
                return;
            }

            Repository repository = repoOpt.get();

            // Eliminar branch de BD
            branchService.deleteBranch(repository.getId(), ref);

            log.info("Branch deleted: {} from repo {}", ref, repoFullName);

        } catch (Exception e) {
            log.error("Error handling delete event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to handle delete event", e);
        }
    }

    /**
     * Handle pull request events
     */
    private void handlePullRequestEvent(JsonNode json) {
        String action = json.path("action").asText();
        int prNumber = json.path("number").asInt();
        String repoFullName = json.path("repository").path("full_name").asText();
        String prTitle = json.path("pull_request").path("title").asText();

        log.info("Pull request event - Repo: {}, PR: #{}, Action: {}, Title: {}",
                repoFullName, prNumber, action, prTitle);

        try {
            // Buscar el repositorio en BD
            Repository repository = repositoryRepository.findByFullName(repoFullName)
                    .orElseThrow(() -> new RuntimeException("Repository not found: " + repoFullName));

            // Opción 1: Usar mapper con JsonNode (más directo)
            PullRequest pullRequest = pullRequestRepository.findByRepoIdAndNumber(repository.getId(), prNumber)
                    .orElse(null);

            if (pullRequest == null) {
                // Crear nuevo PR usando el mapper
                pullRequest = pullRequestMapper.fromJsonNode(json, repository);
                log.info("Creating new Pull Request #{} in repo {}", prNumber, repoFullName);
            } else {
                // Actualizar PR existente - Convertir a DTO y usar mapper
                try {
                    PullRequestEventDTO dto = objectMapper.treeToValue(json, PullRequestEventDTO.class);
                    pullRequestMapper.updateEntity(pullRequest, dto);
                    log.info("Updating existing Pull Request #{} in repo {}", prNumber, repoFullName);
                } catch (Exception e) {
                    // Fallback: usar método directo con JsonNode
                    log.warn("Failed to convert to DTO, using direct mapping: {}", e.getMessage());
                    PullRequest updatedPr = pullRequestMapper.fromJsonNode(json, repository);
                    pullRequest.setGithubPrId(updatedPr.getGithubPrId());
                    pullRequest.setNodeId(updatedPr.getNodeId());
                    pullRequest.setState(updatedPr.getState());
                    pullRequest.setTitle(updatedPr.getTitle());
                    pullRequest.setBody(updatedPr.getBody());
                    pullRequest.setUserLogin(updatedPr.getUserLogin());
                    pullRequest.setUserId(updatedPr.getUserId());
                    pullRequest.setHeadRef(updatedPr.getHeadRef());
                    pullRequest.setHeadSha(updatedPr.getHeadSha());
                    pullRequest.setBaseRef(updatedPr.getBaseRef());
                    pullRequest.setBaseSha(updatedPr.getBaseSha());
                    pullRequest.setDraft(updatedPr.getDraft());
                    pullRequest.setMerged(updatedPr.getMerged());
                    pullRequest.setMergeable(updatedPr.getMergeable());
                    pullRequest.setMergedBy(updatedPr.getMergedBy());
                    pullRequest.setMergedAt(updatedPr.getMergedAt());
                    pullRequest.setClosedAt(updatedPr.getClosedAt());
                    pullRequest.setHtmlUrl(updatedPr.getHtmlUrl());
                }
            }

            // Guardar en BD
            pullRequestRepository.save(pullRequest);
            log.info("Pull request saved/updated: PR #{} in repo {} - Action: {}, State: {}",
                    prNumber, repoFullName, action, pullRequest.getState());

            // Notificar a C# Service
            csharpNotificationService.notifyPullRequestUpdated(
                    repository.getId().toString(),
                    prNumber,
                    action,
                    pullRequest.getState()
            );

        } catch (Exception e) {
            log.error("Error handling pull request event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to handle pull request event", e);
        }
    }

    /**
     * Handle issues events
     */
    private void handleIssuesEvent(JsonNode json) {
        String action = json.path("action").asText();
        int issueNumber = json.path("issue").path("number").asInt();
        String repoFullName = json.path("repository").path("full_name").asText();
        String issueTitle = json.path("issue").path("title").asText();

        log.info("Issues event - Repo: {}, Issue: #{}, Action: {}, Title: {}",
                repoFullName, issueNumber, action, issueTitle);

        try {
            // Buscar el repositorio en BD
            Repository repository = repositoryRepository.findByFullName(repoFullName)
                    .orElseThrow(() -> new RuntimeException("Repository not found: " + repoFullName));

            // Buscar o crear issue en BD usando el mapper
            GithubIssue githubIssue = githubIssueRepository.findByRepoIdAndNumber(repository.getId(), issueNumber)
                    .orElse(null);

            if (githubIssue == null) {
                // Crear nuevo issue usando el mapper
                githubIssue = issueMapper.fromJsonNode(json, repository);
                log.info("Creating new GitHub Issue #{} in repo {}", issueNumber, repoFullName);
            } else {
                // Actualizar issue existente - Convertir a DTO y usar mapper
                try {
                    IssueEventDTO dto = objectMapper.treeToValue(json, IssueEventDTO.class);
                    issueMapper.updateEntity(githubIssue, dto);
                    log.info("Updating existing GitHub Issue #{} in repo {} - Action: {}",
                            issueNumber, repoFullName, action);
                } catch (Exception e) {
                    // Fallback: usar método directo con JsonNode
                    log.warn("Failed to convert to DTO, using direct mapping: {}", e.getMessage());
                    GithubIssue updatedIssue = issueMapper.fromJsonNode(json, repository);
                    githubIssue.setGithubIssueId(updatedIssue.getGithubIssueId());
                    githubIssue.setNodeId(updatedIssue.getNodeId());
                    githubIssue.setState(updatedIssue.getState());
                    githubIssue.setTitle(updatedIssue.getTitle());
                    githubIssue.setBody(updatedIssue.getBody());
                    githubIssue.setUserLogin(updatedIssue.getUserLogin());
                    githubIssue.setUserId(updatedIssue.getUserId());
                    githubIssue.setLabels(updatedIssue.getLabels());
                    githubIssue.setAssignees(updatedIssue.getAssignees());
                    githubIssue.setMilestone(updatedIssue.getMilestone());
                    githubIssue.setLocked(updatedIssue.getLocked());
                    githubIssue.setCommentsCount(updatedIssue.getCommentsCount());
                    githubIssue.setClosedAt(updatedIssue.getClosedAt());
                    githubIssue.setHtmlUrl(updatedIssue.getHtmlUrl());
                }
            }

            // Guardar en BD
            githubIssueRepository.save(githubIssue);
            log.info("GitHub issue saved/updated: Issue #{} in repo {} - Action: {}, State: {}",
                    issueNumber, repoFullName, action, githubIssue.getState());

            // Notificar a C# Service
            csharpNotificationService.notifyIssueUpdated(
                    repository.getId().toString(),
                    issueNumber,
                    action,
                    githubIssue.getState()
            );

        } catch (Exception e) {
            log.error("Error handling issues event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to handle issues event", e);
        }
    }

    /**
     * Handle ping events (GitHub sends this when webhook is created)
     */
    private void handlePingEvent(JsonNode json) {
        String zen = json.path("zen").asText();
        int hookId = json.path("hook_id").asInt();

        log.info("Ping event received - HookId: {}, Zen: {}", hookId, zen);
    }
}

