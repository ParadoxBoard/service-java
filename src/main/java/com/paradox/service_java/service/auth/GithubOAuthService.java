package com.paradox.service_java.service.auth;

import com.paradox.service_java.dto.auth.GithubTokenResponse;
import com.paradox.service_java.dto.auth.GithubUserDTO;
import com.paradox.service_java.exception.GithubAuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para manejar la autenticación con GitHub OAuth
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GithubOAuthService {

    private final WebClient webClient;

    @Value("${github.oauth.client-id}")
    private String clientId;

    @Value("${github.oauth.client-secret}")
    private String clientSecret;

    @Value("${github.oauth.redirect-uri}")
    private String redirectUri;

    @Value("${github.oauth.token-uri}")
    private String tokenUri;

    @Value("${github.oauth.user-info-uri}")
    private String userInfoUri;

    /**
     * Intercambia el código de autorización por un access token
     */
    public String exchangeCodeForAccessToken(String code) {
        log.info("Exchanging authorization code for access token");

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("client_id", clientId);
        requestBody.put("client_secret", clientSecret);
        requestBody.put("code", code);
        requestBody.put("redirect_uri", redirectUri);

        GithubTokenResponse response = webClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> {
                    log.error("Error getting access token from GitHub: {}", clientResponse.statusCode());
                    return Mono.error(new GithubAuthException("Failed to get access token from GitHub"));
                })
                .bodyToMono(GithubTokenResponse.class)
                .block();

        if (response == null || response.getAccessToken() == null) {
            log.error("GitHub token response is null or missing access token");
            throw new GithubAuthException("Invalid response from GitHub");
        }

        if (response.getError() != null) {
            log.error("GitHub OAuth error: {} - {}", response.getError(), response.getErrorDescription());
            throw new GithubAuthException("GitHub OAuth error: " + response.getError());
        }

        log.info("Successfully obtained access token from GitHub");
        return response.getAccessToken();
    }

    /**
     * Obtiene los datos del usuario desde la API de GitHub
     */
    public GithubUserDTO fetchUserData(String accessToken) {
        log.info("Fetching user data from GitHub API");

        GithubUserDTO user = webClient.get()
                .uri(userInfoUri)
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> {
                    log.error("Error fetching user data from GitHub: {}", clientResponse.statusCode());
                    return Mono.error(new GithubAuthException("Failed to fetch user data from GitHub"));
                })
                .bodyToMono(GithubUserDTO.class)
                .block();

        if (user == null) {
            log.error("GitHub user response is null");
            throw new GithubAuthException("Failed to get user data from GitHub");
        }

        log.info("Successfully fetched user data for GitHub user: {}", user.getLogin());
        return user;
    }

    /**
     * Genera la URL de autorización de GitHub
     */
    public String getAuthorizationUrl() {
        return String.format(
                "https://github.com/login/oauth/authorize?client_id=%s&redirect_uri=%s&scope=read:user user:email",
                clientId,
                redirectUri
        );
    }
}

