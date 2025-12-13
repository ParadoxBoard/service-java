package com.paradox.service_java.service;

import com.paradox.service_java.util.GitHubJwtGenerator;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class GitHubApiService {
    private final WebClient webClient;
    private final GitHubJwtGenerator jwtGenerator;

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_REF = new ParameterizedTypeReference<>() {};

    public GitHubApiService(WebClient.Builder webClientBuilder, GitHubJwtGenerator jwtGenerator) {
        this.webClient = webClientBuilder.baseUrl("https://api.github.com").build();
        this.jwtGenerator = jwtGenerator;
    }

    private HttpHeaders defaultAppHeaders(String bearer) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + bearer);
        headers.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
        headers.set(HttpHeaders.USER_AGENT, "paradoxboard-service");
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        return headers;
    }

    public Map<String, Object> getInstallation(Long installationId) {
        String jwt = jwtGenerator.generateJwt();
        try {
            return this.webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/app/installations/{id}").build(installationId))
                    .headers(h -> h.addAll(defaultAppHeaders(jwt)))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(MAP_REF)
                    .block();
        } catch (WebClientResponseException ex) {
            throw new IllegalStateException(ex.getStatusCode().value() + " " + ex.getStatusText() + " from GET https://api.github.com/app/installations/" + installationId + " - " + ex.getResponseBodyAsString(), ex);
        }
    }

    /**
     * Obtiene los Pull Requests de un repositorio
     * @param repoFullName Nombre completo del repo (owner/repo)
     * @param state Estado de los PRs: open, closed, all
     * @param token Token de instalación
     */
    public List<Map<String, Object>> getPullRequests(String repoFullName, String state, String token) {
        try {
            return this.webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/pulls")
                            .queryParam("state", state)
                            .queryParam("per_page", 100)
                            .build(repoFullName.split("/")[0], repoFullName.split("/")[1]))
                    .headers(h -> h.addAll(defaultInstallationHeaders(token)))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .block();
        } catch (WebClientResponseException ex) {
            throw new IllegalStateException("Error fetching PRs from " + repoFullName + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Obtiene los Issues de un repositorio
     * @param repoFullName Nombre completo del repo (owner/repo)
     * @param state Estado de los issues: open, closed, all
     * @param token Token de instalación
     */
    public List<Map<String, Object>> getIssues(String repoFullName, String state, String token) {
        try {
            return this.webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/issues")
                            .queryParam("state", state)
                            .queryParam("per_page", 100)
                            .build(repoFullName.split("/")[0], repoFullName.split("/")[1]))
                    .headers(h -> h.addAll(defaultInstallationHeaders(token)))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .block();
        } catch (WebClientResponseException ex) {
            throw new IllegalStateException("Error fetching issues from " + repoFullName + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Headers por defecto para requests con token de instalación
     */
    private HttpHeaders defaultInstallationHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        headers.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
        headers.set(HttpHeaders.USER_AGENT, "paradoxboard-service");
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        return headers;
    }
}
