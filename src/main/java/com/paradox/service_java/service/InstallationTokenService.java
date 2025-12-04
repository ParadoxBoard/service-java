package com.paradox.service_java.service;

import com.paradox.service_java.util.GitHubJwtGenerator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class InstallationTokenService {

    private final WebClient webClient;
    private final GitHubJwtGenerator jwtGenerator;

    public InstallationTokenService(WebClient.Builder webClientBuilder, GitHubJwtGenerator jwtGenerator) {
        this.webClient = webClientBuilder.baseUrl("https://api.github.com").build();
        this.jwtGenerator = jwtGenerator;
    }

    public String createInstallationToken(Long installationId) throws Exception {
        String jwt = jwtGenerator.generateJwt();
        Map response = this.webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/app/installations/{id}/access_tokens").build(installationId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("token")) {
            throw new IllegalStateException("Failed to create installation token");
        }
        return (String) response.get("token");
    }
}

