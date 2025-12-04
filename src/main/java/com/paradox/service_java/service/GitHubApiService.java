package com.paradox.service_java.service;

import com.paradox.service_java.util.GitHubJwtGenerator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class GitHubApiService {
    private final WebClient webClient;
    private final GitHubJwtGenerator jwtGenerator;

    public GitHubApiService(WebClient.Builder webClientBuilder, GitHubJwtGenerator jwtGenerator) {
        this.webClient = webClientBuilder.baseUrl("https://api.github.com").build();
        this.jwtGenerator = jwtGenerator;
    }

    public Map getInstallation(Long installationId) throws Exception {
        String jwt = jwtGenerator.generateJwt();
        Map response = this.webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/app/installations/{id}").build(installationId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        return response;
    }

    public Map getUserWithInstallationToken(String installationToken) {
        Map response = this.webClient.get()
                .uri("/user")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + installationToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        return response;
    }
}

