package com.paradox.service_java.service;

import com.paradox.service_java.model.Installation;
import com.paradox.service_java.model.Repository;
import com.paradox.service_java.repository.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Servicio para sincronización inicial y gestión de repositorios
 */
@Slf4j
@Service
public class SyncService {

    private final WebClient webClient;
    private final InstallationTokenService installationTokenService;
    private final RepositoryRepository repositoryRepository;

    // Constructor que configura WebClient con base URL correcta
    public SyncService(WebClient.Builder webClientBuilder,
                       InstallationTokenService installationTokenService,
                       RepositoryRepository repositoryRepository) {
        this.webClient = webClientBuilder.baseUrl("https://api.github.com").build();
        this.installationTokenService = installationTokenService;
        this.repositoryRepository = repositoryRepository;
    }

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_REF =
        new ParameterizedTypeReference<>() {};

    /**
     * Sincronización inicial: obtiene y guarda todos los repositorios de una instalación
     */
    @Transactional
    public List<Repository> syncInitial(Long installationId, Installation installation) {
        log.info("Starting initial sync for installation: {}", installationId);

        try {
            // Obtener token de instalación
            String token = installationTokenService.getInstallationToken(installationId);

            // Obtener repositorios de la instalación
            List<Map<String, Object>> reposData = fetchInstallationRepositories(token);

            log.info("Found {} repositories for installation {}", reposData.size(), installationId);

            // Guardar o actualizar cada repositorio
            List<Repository> savedRepos = new ArrayList<>();
            for (Map<String, Object> repoData : reposData) {
                try {
                    Repository repo = createOrUpdateRepository(repoData, installation);
                    savedRepos.add(repo);
                } catch (Exception e) {
                    log.error("Error processing repository: {}", repoData.get("full_name"), e);
                }
            }

            log.info("Successfully synced {} repositories for installation {}",
                    savedRepos.size(), installationId);

            return savedRepos;

        } catch (Exception e) {
            log.error("Error during initial sync for installation {}: {}", installationId, e.getMessage(), e);
            throw new RuntimeException("Failed to sync repositories", e);
        }
    }

    /**
     * Obtiene la lista de repositorios accesibles por la instalación
     */
    private List<Map<String, Object>> fetchInstallationRepositories(String token) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("/installation/repositories")
                    .headers(h -> h.addAll(createAuthHeaders(token)))
                    .retrieve()
                    .bodyToMono(MAP_REF)
                    .block();

            if (response != null && response.containsKey("repositories")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> repos = (List<Map<String, Object>>) response.get("repositories");
                return repos != null ? repos : new ArrayList<>();
            }

            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error fetching installation repositories: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch repositories", e);
        }
    }

    /**
     * Crea o actualiza un repositorio desde los datos de GitHub
     */
    @Transactional
    public Repository createOrUpdateRepository(Map<String, Object> repoData, Installation installation) {
        Long githubRepoId = extractLong(repoData, "id");

        if (githubRepoId == null) {
            throw new IllegalArgumentException("Repository ID is required");
        }

        Optional<Repository> existing = repositoryRepository.findByGithubRepoId(githubRepoId);

        Repository repository;
        if (existing.isPresent()) {
            repository = existing.get();
            log.debug("Updating existing repository: {}", githubRepoId);
        } else {
            repository = new Repository();
            repository.setGithubRepoId(githubRepoId);
            repository.setInstallation(installation);
            log.debug("Creating new repository: {}", githubRepoId);
        }

        // Datos básicos
        repository.setNodeId(extractString(repoData, "node_id"));
        repository.setName(extractString(repoData, "name"));
        repository.setFullName(extractString(repoData, "full_name"));
        repository.setDescription(extractString(repoData, "description"));
        repository.setPrivateRepo(extractBoolean(repoData, "private"));
        repository.setFork(extractBoolean(repoData, "fork"));
        repository.setArchived(extractBoolean(repoData, "archived"));
        repository.setDisabled(extractBoolean(repoData, "disabled"));

        // Owner
        Map<String, Object> owner = extractMap(repoData, "owner");
        if (owner != null) {
            repository.setOwnerLogin(extractString(owner, "login"));
            repository.setOwnerType(extractString(owner, "type"));
        }

        // URLs
        repository.setHtmlUrl(extractString(repoData, "html_url"));
        repository.setCloneUrl(extractString(repoData, "clone_url"));
        repository.setSshUrl(extractString(repoData, "ssh_url"));

        // Metadata
        repository.setDefaultBranch(extractString(repoData, "default_branch"));
        repository.setLanguage(extractString(repoData, "language"));

        List<String> topics = extractStringList(repoData, "topics");
        if (topics != null) {
            repository.setTopics(topics);
        }

        // Timestamps
        String pushedAtStr = extractString(repoData, "pushed_at");
        if (pushedAtStr != null) {
            try {
                repository.setPushedAt(OffsetDateTime.parse(pushedAtStr));
            } catch (Exception e) {
                log.warn("Could not parse pushed_at: {}", pushedAtStr);
            }
        }

        return repositoryRepository.save(repository);
    }

    /**
     * Crea headers de autenticación para llamadas a GitHub API
     */
    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        headers.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
        headers.set(HttpHeaders.USER_AGENT, "paradoxboard-service");
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        return headers;
    }

    // ===== Métodos auxiliares de extracción =====

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
            log.warn("Could not parse long from key {}: {}", key, value);
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

    @SuppressWarnings("unchecked")
    private List<String> extractStringList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return null;
    }
}

