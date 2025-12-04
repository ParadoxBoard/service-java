package com.paradox.service_java.controller;

import com.paradox.service_java.dto.auth.AuthResponse;
import com.paradox.service_java.service.auth.AuthService;
import com.paradox.service_java.service.auth.GithubOAuthService;
import com.paradox.service_java.service.GitHubApiService;
import com.paradox.service_java.service.InstallationTokenService;
import com.paradox.service_java.dto.GithubRegisterRequest;
import com.paradox.service_java.service.UserService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * Controlador para manejar la autenticación con GitHub OAuth
 */
@Slf4j
@RestController
@RequestMapping("/auth/github")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;
    private final GithubOAuthService githubOAuthService;
    private final GitHubApiService gitHubApiService;
    private final InstallationTokenService installationTokenService;
    private final UserService userService;

    /**
     * Endpoint de login - Redirige al usuario a GitHub para autenticación
     * GET /auth/github/login
     */
    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        log.info("Redirecting to GitHub OAuth login");
        
        String authorizationUrl = githubOAuthService.getAuthorizationUrl();
        
        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(authorizationUrl))
                .build();
    }

    /**
     * Callback de GitHub OAuth - Recibe el código de autorización
     * GET /auth/github/callback?code=xxx
     */
    @GetMapping("/callback")
    public ResponseEntity<AuthResponse> callback(
            @RequestParam("code") @NotBlank String code
    ) {
        log.info("Processing GitHub OAuth callback with code");

        try {
            AuthResponse response = authService.handleGithubCallback(code);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing GitHub callback: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Endpoint alternativo para obtener la URL de autorización (útil para frontends SPA)
     * GET /auth/github/authorize-url
     */
    @GetMapping("/authorize-url")
    public ResponseEntity<AuthUrlResponse> getAuthorizationUrl() {
        String url = githubOAuthService.getAuthorizationUrl();
        
        return ResponseEntity.ok(new AuthUrlResponse(url));
    }

    /**
     * DTO para la respuesta de la URL de autorización
     */
    public record AuthUrlResponse(String authorizationUrl) {}

    /**
     * Endpoint para registro de usuarios usando GitHub Installation ID
     * POST /auth/github/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerWithInstallation(@RequestBody GithubRegisterRequest request) {
        try {
            Long installationId = request.getInstallationId();
            // Obtener informacion de la installation (owner)
            var installInfo = gitHubApiService.getInstallation(installationId);
            // crear token de installation
            String installToken = installationTokenService.createInstallationToken(installationId);
            // obtener usuario desde installation token
            var ghUser = gitHubApiService.getUserWithInstallationToken(installToken);

            // extraer campos
            Long ghId = null;
            if (installInfo != null && installInfo.get("account") instanceof java.util.Map) {
                Object idObj = ((java.util.Map)installInfo.get("account")).get("id");
                if (idObj != null) ghId = Long.valueOf(String.valueOf(idObj));
            }
            String login = ghUser != null ? String.valueOf(ghUser.get("login")) : null;
            String email = ghUser != null ? String.valueOf(ghUser.get("email")) : null;
            String avatar = ghUser != null ? String.valueOf(ghUser.get("avatar_url")) : null;
            String name = ghUser != null ? String.valueOf(ghUser.get("name")) : null;

            var userResp = userService.createOrUpdateFromGithub(ghId, login, email, installationId, avatar, name);
            return ResponseEntity.created(URI.create("/api/users/" + userResp.getId())).body(userResp);
        } catch (Exception e) {
            log.error("Error in registerWithInstallation: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
