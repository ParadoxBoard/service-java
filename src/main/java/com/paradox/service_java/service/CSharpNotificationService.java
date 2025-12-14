package com.paradox.service_java.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Servicio para notificar eventos a C# Service
 * Permite integración en tiempo real con el core de la aplicación
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CSharpNotificationService {

    private final WebClient webClient;

    @Value("${csharp.service.url:http://localhost:4001}")
    private String csharpServiceUrl;

    /**
     * Notifica a C# Service cuando hay un nuevo commit
     */
    public void notifyCommitCreated(String repoId, String commitSha, String message, String author) {
        log.info("Notifying C# Service about new commit: {}", commitSha);

        Map<String, Object> payload = Map.of(
            "event", "commit.created",
            "repoId", repoId,
            "commitSha", commitSha,
            "message", message,
            "author", author,
            "timestamp", System.currentTimeMillis()
        );

        sendNotification("/api/tasks/sync", payload);
    }

    /**
     * Notifica a C# Service cuando hay un nuevo PR
     */
    public void notifyPullRequestUpdated(String repoId, Integer prNumber, String action, String state) {
        log.info("Notifying C# Service about PR #{}: {}", prNumber, action);

        Map<String, Object> payload = Map.of(
            "event", "pull_request." + action,
            "repoId", repoId,
            "prNumber", prNumber,
            "state", state,
            "timestamp", System.currentTimeMillis()
        );

        sendNotification("/api/tasks/sync", payload);
    }

    /**
     * Notifica a C# Service cuando hay un nuevo Issue
     */
    public void notifyIssueUpdated(String repoId, Integer issueNumber, String action, String state) {
        log.info("Notifying C# Service about Issue #{}: {}", issueNumber, action);

        Map<String, Object> payload = Map.of(
            "event", "issue." + action,
            "repoId", repoId,
            "issueNumber", issueNumber,
            "state", state,
            "timestamp", System.currentTimeMillis()
        );

        sendNotification("/api/tasks/sync", payload);
    }

    /**
     * Notifica a C# Service cuando hay un nuevo Branch
     */
    public void notifyBranchCreated(String repoId, String branchName, String sha) {
        log.info("Notifying C# Service about new branch: {}", branchName);

        Map<String, Object> payload = Map.of(
            "event", "branch.created",
            "repoId", repoId,
            "branchName", branchName,
            "sha", sha,
            "timestamp", System.currentTimeMillis()
        );

        sendNotification("/api/tasks/sync", payload);
    }

    /**
     * Envía notificación genérica a C# Service
     */
    private void sendNotification(String endpoint, Map<String, Object> payload) {
        webClient.post()
            .uri(csharpServiceUrl + endpoint)
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(Void.class)
            .onErrorResume(error -> {
                log.error("Error notifying C# Service: {}", error.getMessage());
                return Mono.empty();
            })
            .subscribe();
    }
}

