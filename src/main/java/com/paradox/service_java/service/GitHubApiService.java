package com.paradox.service_java.service;

import com.paradox.service_java.util.GitHubJwtGenerator;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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
}
