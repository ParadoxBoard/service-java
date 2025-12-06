package com.paradox.service_java.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Service to handle GitHub webhook events
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final ObjectMapper objectMapper;

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
    public void processWebhook(String eventType, String payload) {
        try {
            JsonNode json = objectMapper.readTree(payload);

            switch (eventType) {
                case "installation" -> handleInstallationEvent(json);
                case "installation_repositories" -> handleInstallationRepositoriesEvent(json);
                case "push" -> handlePushEvent(json);
                case "pull_request" -> handlePullRequestEvent(json);
                case "issues" -> handleIssuesEvent(json);
                case "ping" -> handlePingEvent(json);
                default -> log.info("Unhandled webhook event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing webhook payload: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process webhook", e);
        }
    }

    /**
     * Handle installation events (created/deleted)
     */
    private void handleInstallationEvent(JsonNode json) {
        String action = json.path("action").asText();
        long installationId = json.path("installation").path("id").asLong();
        String accountLogin = json.path("installation").path("account").path("login").asText();

        log.info("Installation event - Action: {}, InstallationId: {}, Account: {}",
                action, installationId, accountLogin);

        if ("created".equals(action)) {
            // TODO: Create or update Installation in DB
            log.info("New installation created: {} for account: {}", installationId, accountLogin);
        } else if ("deleted".equals(action)) {
            // TODO: Mark installation as deleted in DB
            log.info("Installation deleted: {} for account: {}", installationId, accountLogin);
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
        String prTitle = json.path("pull_request").path("title").asText();

        log.info("Pull request event - Repo: {}, PR: #{}, Action: {}, Title: {}",
                repoFullName, prNumber, action, prTitle);

        // TODO: Process PR event and update boards/tasks
        // Common actions: opened, closed, merged, synchronized, reopened
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

        // TODO: Process issue event and update boards/tasks
        // Common actions: opened, closed, reopened, edited, assigned, labeled
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

