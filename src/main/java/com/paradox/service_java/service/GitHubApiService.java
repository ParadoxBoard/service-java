package com.paradox.service_java.service;

import com.paradox.service_java.util.GitHubJwtGenerator;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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

    public Map<String, Object> getInstallation(Long installationId) {
        String jwt = jwtGenerator.generateJwt();
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/app/installations/{id}").build(installationId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(MAP_REF)
                .block();
    }

    public Map<String, Object> getUserWithInstallationToken(String installationToken) {
        return this.webClient.get()
                .uri("/user")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + installationToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(MAP_REF)
                .block();
    }
}
