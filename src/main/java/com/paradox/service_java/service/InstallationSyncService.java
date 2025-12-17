package com.paradox.service_java.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paradox.service_java.model.Installation;
import com.paradox.service_java.model.Repository;
import com.paradox.service_java.model.WebhookLog;
import com.paradox.service_java.repository.InstallationRepository;
import com.paradox.service_java.repository.RepositoryRepository;
import com.paradox.service_java.repository.WebhookLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Servicio para sincronización manual de instalaciones y repositorios
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstallationSyncService {

    private final WebhookLogRepository webhookLogRepository;
    private final InstallationRepository installationRepository;
    private final RepositoryRepository repositoryRepository;
    private final ObjectMapper objectMapper;

    /**
     * Sincroniza repositorios desde los logs de webhooks de tipo installation
     */
    @Transactional
    public Map<String, Object> syncRepositoriesFromWebhookLogs() {
        log.info("Starting repository sync from webhook logs");

        List<WebhookLog> installationWebhooks = webhookLogRepository
                .findByEventTypeOrderByCreatedAtDesc("installation");

        if (installationWebhooks.isEmpty()) {
            log.warn("No installation webhooks found in logs");
            return Map.of(
                    "status", "no_data",
                    "message", "No installation webhooks found"
            );
        }

        int processedWebhooks = 0;
        int savedRepositories = 0;
        List<String> errors = new ArrayList<>();

        for (WebhookLog webhook : installationWebhooks) {
            try {
                Map<String, Object> payload = webhook.getRequestPayload();
                String action = extractString(payload, "action");

                if (!"created".equals(action)) {
                    continue; // Solo procesar instalaciones creadas
                }

                Map<String, Object> installationData = extractMap(payload, "installation");
                if (installationData == null) {
                    log.warn("No installation data in webhook {}", webhook.getId());
                    continue;
                }

                Long installationId = extractLong(installationData, "id");
                Optional<Installation> installationOpt = installationRepository.findByInstallationId(installationId);

                if (installationOpt.isEmpty()) {
                    log.warn("Installation {} not found in database", installationId);
                    continue;
                }

                Installation installation = installationOpt.get();

                // Procesar repositorios del payload
                Object repositoriesObj = payload.get("repositories");
                if (repositoriesObj instanceof List) {
                    List<?> repositories = (List<?>) repositoriesObj;

                    for (Object repoObj : repositories) {
                        if (repoObj instanceof Map) {
                            try {
                                Map<String, Object> repoData = (Map<String, Object>) repoObj;
                                Long githubRepoId = extractLong(repoData, "id");

                                // Verificar si ya existe
                                Optional<Repository> existingRepo = repositoryRepository.findByGithubRepoId(githubRepoId);

                                if (existingRepo.isEmpty()) {
                                    Repository newRepo = createRepositoryFromData(installation, repoData);
                                    repositoryRepository.save(newRepo);
                                    savedRepositories++;
                                    log.info("Saved repository: {} ({})", newRepo.getFullName(), githubRepoId);
                                }
                            } catch (Exception e) {
                                log.error("Error processing repository: {}", e.getMessage());
                                errors.add("Repository error: " + e.getMessage());
                            }
                        }
                    }
                }

                processedWebhooks++;

            } catch (Exception e) {
                log.error("Error processing webhook {}: {}", webhook.getId(), e.getMessage(), e);
                errors.add("Webhook " + webhook.getId() + ": " + e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("processedWebhooks", processedWebhooks);
        result.put("savedRepositories", savedRepositories);
        if (!errors.isEmpty()) {
            result.put("errors", errors);
        }

        log.info("Repository sync completed: {} webhooks processed, {} repositories saved",
                processedWebhooks, savedRepositories);

        return result;
    }

    /**
     * Sincroniza repositorios para una instalación específica
     */
    @Transactional
    public Map<String, Object> syncRepositoriesForInstallation(Long installationId) {
        log.info("Syncing repositories for installation: {}", installationId);

        Optional<Installation> installationOpt = installationRepository.findByInstallationId(installationId);
        if (installationOpt.isEmpty()) {
            return Map.of(
                    "status", "not_found",
                    "message", "Installation not found: " + installationId
            );
        }

        Installation installation = installationOpt.get();

        // Buscar el webhook más reciente de esta instalación
        List<WebhookLog> webhooks = webhookLogRepository.findByEventTypeOrderByCreatedAtDesc("installation");

        WebhookLog targetWebhook = null;
        for (WebhookLog webhook : webhooks) {
            try {
                Map<String, Object> payload = webhook.getRequestPayload();
                Map<String, Object> installationData = extractMap(payload, "installation");
                if (installationData != null) {
                    Long webhookInstallationId = extractLong(installationData, "id");
                    if (installationId.equals(webhookInstallationId)) {
                        targetWebhook = webhook;
                        break;
                    }
                }
            } catch (Exception e) {
                log.warn("Error checking webhook {}: {}", webhook.getId(), e.getMessage());
            }
        }

        if (targetWebhook == null) {
            return Map.of(
                    "status", "no_webhook",
                    "message", "No installation webhook found for installation: " + installationId
            );
        }

        // Procesar repositorios del webhook encontrado
        int savedCount = 0;
        try {
            Map<String, Object> payload = targetWebhook.getRequestPayload();
            Object repositoriesObj = payload.get("repositories");

            if (repositoriesObj instanceof List) {
                List<?> repositories = (List<?>) repositoriesObj;

                for (Object repoObj : repositories) {
                    if (repoObj instanceof Map) {
                        try {
                            Map<String, Object> repoData = (Map<String, Object>) repoObj;
                            Long githubRepoId = extractLong(repoData, "id");

                            Optional<Repository> existingRepo = repositoryRepository.findByGithubRepoId(githubRepoId);

                            if (existingRepo.isEmpty()) {
                                Repository newRepo = createRepositoryFromData(installation, repoData);
                                repositoryRepository.save(newRepo);
                                savedCount++;
                                log.info("Saved repository: {} ({})", newRepo.getFullName(), githubRepoId);
                            }
                        } catch (Exception e) {
                            log.error("Error processing repository: {}", e.getMessage());
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error syncing repositories: {}", e.getMessage(), e);
            return Map.of(
                    "status", "error",
                    "message", e.getMessage()
            );
        }

        return Map.of(
                "status", "success",
                "installationId", installationId,
                "accountLogin", installation.getAccountLogin(),
                "savedRepositories", savedCount
        );
    }

    /**
     * Crea un objeto Repository desde los datos del webhook
     */
    private Repository createRepositoryFromData(Installation installation, Map<String, Object> repoData) {
        Long githubRepoId = extractLong(repoData, "id");
        String nodeId = extractString(repoData, "node_id");
        String name = extractString(repoData, "name");
        String fullName = extractString(repoData, "full_name");
        Boolean isPrivate = extractBoolean(repoData, "private");

        String ownerLogin = installation.getAccountLogin();
        if (fullName != null && fullName.contains("/")) {
            ownerLogin = fullName.split("/")[0];
        }

        return Repository.builder()
                .installation(installation)
                .githubRepoId(githubRepoId)
                .nodeId(nodeId)
                .name(name)
                .fullName(fullName)
                .ownerLogin(ownerLogin)
                .privateRepo(isPrivate != null ? isPrivate : false)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    // Métodos auxiliares
    private String extractString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    private Long extractLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean extractBoolean(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.valueOf(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }
}

