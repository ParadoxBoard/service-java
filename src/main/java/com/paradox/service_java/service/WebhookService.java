package com.paradox.service_java.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final RepositoryRepository repositoryRepository;
    private final PullRequestRepository pullRequestRepository;
    private final GithubIssueRepository githubIssueRepository;

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
                installationService.createOrUpdateFromGitHub(installationData);
                log.info("Installation saved: {} for account: {}", installationId, accountLogin);

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
     */
    private void handleInstallationRepositoriesEvent(JsonNode json) {
        String action = json.path("action").asText();
        long installationId = json.path("installation").path("id").asLong();

        log.info("Installation repositories event - Action: {}, InstallationId: {}", action, installationId);

        // TODO: Process repositories added or removed
        if ("added".equals(action)) {
            JsonNode addedRepos = json.path("repositories_added");
            log.info("Repositories added: {}", addedRepos.size());
        } else if ("removed".equals(action)) {
            JsonNode removedRepos = json.path("repositories_removed");
            log.info("Repositories removed: {}", removedRepos.size());
        }
    }

    /**
     * Handle push events
     */
    private void handlePushEvent(JsonNode json) {
        String ref = json.path("ref").asText();
        String repoFullName = json.path("repository").path("full_name").asText();
        int commitCount = json.path("commits").size();

        log.info("Push event - Repo: {}, Ref: {}, Commits: {}", repoFullName, ref, commitCount);

        // TODO: Process push event and update boards/tasks
        // 1. Find repo in DB
        // 2. Find associated boards
        // 3. Update tasks based on commit messages
    }

    /**
     * Handle pull request events
     */
    private void handlePullRequestEvent(JsonNode json) {
        String action = json.path("action").asText();
        int prNumber = json.path("number").asInt();
        String repoFullName = json.path("repository").path("full_name").asText();
        JsonNode prNode = json.path("pull_request");
        String prTitle = prNode.path("title").asText();

        log.info("Pull request event - Repo: {}, PR: #{}, Action: {}, Title: {}",
                repoFullName, prNumber, action, prTitle);

        try {
            // Buscar el repositorio en BD
            Repository repository = repositoryRepository.findByFullName(repoFullName)
                    .orElseThrow(() -> new RuntimeException("Repository not found: " + repoFullName));

            // Extraer datos del PR del payload
            Long githubPrId = prNode.path("id").asLong();
            String nodeId = prNode.path("node_id").asText();
            String state = prNode.path("state").asText();
            String body = prNode.path("body").asText(null);

            JsonNode userNode = prNode.path("user");
            String userLogin = userNode.path("login").asText();
            Long userId = userNode.path("id").asLong();

            JsonNode headNode = prNode.path("head");
            String headRef = headNode.path("ref").asText();
            String headSha = headNode.path("sha").asText();

            JsonNode baseNode = prNode.path("base");
            String baseRef = baseNode.path("ref").asText();
            String baseSha = baseNode.path("sha").asText();

            Boolean draft = prNode.path("draft").asBoolean(false);
            Boolean merged = prNode.path("merged").asBoolean(false);
            Boolean mergeable = prNode.has("mergeable") && !prNode.path("mergeable").isNull()
                    ? prNode.path("mergeable").asBoolean()
                    : null;

            String mergedBy = prNode.has("merged_by") && !prNode.path("merged_by").isNull()
                    ? prNode.path("merged_by").path("login").asText()
                    : null;

            OffsetDateTime mergedAt = prNode.has("merged_at") && !prNode.path("merged_at").isNull()
                    ? OffsetDateTime.parse(prNode.path("merged_at").asText())
                    : null;

            OffsetDateTime closedAt = prNode.has("closed_at") && !prNode.path("closed_at").isNull()
                    ? OffsetDateTime.parse(prNode.path("closed_at").asText())
                    : null;

            String htmlUrl = prNode.path("html_url").asText();

            // Buscar o crear PR en BD
            PullRequest pullRequest = pullRequestRepository.findByRepoIdAndNumber(repository.getId(), prNumber)
                    .orElse(PullRequest.builder()
                            .repo(repository)
                            .number(prNumber)
                            .build());

            // Actualizar campos
            pullRequest.setGithubPrId(githubPrId);
            pullRequest.setNodeId(nodeId);
            pullRequest.setTitle(prTitle);
            pullRequest.setBody(body);
            pullRequest.setUserLogin(userLogin);
            pullRequest.setUserId(userId);
            pullRequest.setHeadRef(headRef);
            pullRequest.setHeadSha(headSha);
            pullRequest.setBaseRef(baseRef);
            pullRequest.setBaseSha(baseSha);
            pullRequest.setDraft(draft);
            pullRequest.setMergeable(mergeable);
            pullRequest.setHtmlUrl(htmlUrl);

            // Actualizar estado según la acción
            switch (action) {
                case "opened":
                case "reopened":
                    pullRequest.setState("open");
                    pullRequest.setMerged(false);
                    break;
                case "closed":
                    if (merged) {
                        pullRequest.setState("merged");
                        pullRequest.setMerged(true);
                        pullRequest.setMergedBy(mergedBy);
                        pullRequest.setMergedAt(mergedAt);
                    } else {
                        pullRequest.setState("closed");
                        pullRequest.setMerged(false);
                    }
                    pullRequest.setClosedAt(closedAt);
                    break;
                case "synchronize":
                    // Actualizar commits (nuevo push al PR)
                    pullRequest.setHeadSha(headSha);
                    break;
                default:
                    // Para otras acciones, mantener estado actual
                    pullRequest.setState(state);
                    pullRequest.setMerged(merged);
                    if (merged) {
                        pullRequest.setMergedBy(mergedBy);
                        pullRequest.setMergedAt(mergedAt);
                    }
                    if (closedAt != null) {
                        pullRequest.setClosedAt(closedAt);
                    }
            }

            // Guardar en BD
            pullRequestRepository.save(pullRequest);
            log.info("Pull request saved/updated: PR #{} in repo {} - Action: {}", prNumber, repoFullName, action);

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
        JsonNode issueNode = json.path("issue");
        int issueNumber = issueNode.path("number").asInt();
        String repoFullName = json.path("repository").path("full_name").asText();
        String issueTitle = issueNode.path("title").asText();

        log.info("Issues event - Repo: {}, Issue: #{}, Action: {}, Title: {}",
                repoFullName, issueNumber, action, issueTitle);

        try {
            // Buscar el repositorio en BD
            Repository repository = repositoryRepository.findByFullName(repoFullName)
                    .orElseThrow(() -> new RuntimeException("Repository not found: " + repoFullName));

            // Extraer datos del issue del payload
            Long githubIssueId = issueNode.path("id").asLong();
            String nodeId = issueNode.path("node_id").asText();
            String state = issueNode.path("state").asText();
            String body = issueNode.path("body").asText(null);

            JsonNode userNode = issueNode.path("user");
            String userLogin = userNode.path("login").asText();
            Long userId = userNode.path("id").asLong();

            // Extraer labels
            List<String> labels = new java.util.ArrayList<>();
            JsonNode labelsNode = issueNode.path("labels");
            if (labelsNode.isArray()) {
                for (JsonNode labelNode : labelsNode) {
                    labels.add(labelNode.path("name").asText());
                }
            }

            // Extraer assignees
            List<String> assignees = new java.util.ArrayList<>();
            JsonNode assigneesNode = issueNode.path("assignees");
            if (assigneesNode.isArray()) {
                for (JsonNode assigneeNode : assigneesNode) {
                    assignees.add(assigneeNode.path("login").asText());
                }
            }

            String milestone = issueNode.has("milestone") && !issueNode.path("milestone").isNull()
                    ? issueNode.path("milestone").path("title").asText()
                    : null;

            Boolean locked = issueNode.path("locked").asBoolean(false);
            Integer commentsCount = issueNode.path("comments").asInt(0);

            OffsetDateTime closedAt = issueNode.has("closed_at") && !issueNode.path("closed_at").isNull()
                    ? OffsetDateTime.parse(issueNode.path("closed_at").asText())
                    : null;

            String htmlUrl = issueNode.path("html_url").asText();

            // Buscar o crear issue en BD
            GithubIssue githubIssue = githubIssueRepository.findByRepoIdAndNumber(repository.getId(), issueNumber)
                    .orElse(GithubIssue.builder()
                            .repo(repository)
                            .number(issueNumber)
                            .build());

            // Actualizar campos
            githubIssue.setGithubIssueId(githubIssueId);
            githubIssue.setNodeId(nodeId);
            githubIssue.setState(state);
            githubIssue.setTitle(issueTitle);
            githubIssue.setBody(body);
            githubIssue.setUserLogin(userLogin);
            githubIssue.setUserId(userId);
            githubIssue.setLabels(labels);
            githubIssue.setAssignees(assignees);
            githubIssue.setMilestone(milestone);
            githubIssue.setLocked(locked);
            githubIssue.setCommentsCount(commentsCount);
            githubIssue.setClosedAt(closedAt);
            githubIssue.setHtmlUrl(htmlUrl);

            // Guardar en BD
            githubIssueRepository.save(githubIssue);
            log.info("GitHub issue saved/updated: Issue #{} in repo {} - Action: {}, State: {}",
                    issueNumber, repoFullName, action, state);

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

