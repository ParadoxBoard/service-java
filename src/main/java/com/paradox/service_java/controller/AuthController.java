package com.paradox.service_java.controller;

import com.paradox.service_java.dto.auth.AuthResponse;
import com.paradox.service_java.service.auth.AuthService;
import com.paradox.service_java.service.auth.GithubOAuthService;
import com.paradox.service_java.service.GitHubApiService;
import com.paradox.service_java.dto.GithubRegisterRequest;
import com.paradox.service_java.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import jakarta.validation.Valid;

/**
 * Controlador para manejar la autenticación con GitHub OAuth
 */
@Slf4j
@RestController
@RequestMapping("/auth/github")
@RequiredArgsConstructor
@Validated
@Tag(name = "Authentication", description = "GitHub OAuth authentication endpoints")
public class AuthController {

    private final AuthService authService;
    private final GithubOAuthService githubOAuthService;
    private final GitHubApiService gitHubApiService;
    private final UserService userService;

    /**
     * Endpoint de login - Redirige al usuario a GitHub para autenticación
     * GET /auth/github/login
     */
    @Operation(summary = "Initiate GitHub OAuth login",
               description = "Redirects user to GitHub authorization page")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "302", description = "Redirect to GitHub OAuth"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
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
    @Operation(summary = "GitHub OAuth callback",
               description = "Handles GitHub OAuth callback and returns JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Missing or invalid code"),
        @ApiResponse(responseCode = "401", description = "GitHub authentication failed"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/callback")
    public ResponseEntity<AuthResponse> callback(
            @Parameter(description = "GitHub authorization code", required = true)
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
    @Operation(summary = "Get GitHub authorization URL",
               description = "Returns the GitHub OAuth authorization URL for frontend applications")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authorization URL returned successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
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
    @Operation(summary = "Register user with GitHub Installation",
               description = "Registers or updates a user using GitHub App Installation ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/register")
    public ResponseEntity<?> registerWithInstallation(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "GitHub installation registration request",
                required = true,
                content = @Content(schema = @Schema(implementation = GithubRegisterRequest.class))
            )
            @Valid @RequestBody GithubRegisterRequest request) {
        try {
            Long installationId = request.getInstallationId();
            // Obtener informacion de la installation (owner)
            Map<String, Object> installInfo = gitHubApiService.getInstallation(installationId);
            // No creamos ni usamos installation token para obtener /user (las installation tokens no representan un usuario
            // y pueden devolver 403). Usamos únicamente installInfo.account (owner) para obtener info pública.

            // extraer campos de forma segura desde installInfo.account
            Long ghId = null;
            String login = null;
            String email = null; // no disponible vía installation token
            String avatar = null;
            String name = null;

            if (installInfo != null) {
                Object accountObj = installInfo.get("account");
                if (accountObj instanceof Map<?, ?> accountMap) {
                    Object idObj = accountMap.get("id");
                    if (idObj != null) {
                        try { ghId = Long.valueOf(String.valueOf(idObj)); } catch (NumberFormatException ignored) {}
                    }
                    Object loginObj = accountMap.get("login");
                    if (loginObj != null) login = String.valueOf(loginObj);
                    Object avatarObj = accountMap.get("avatar_url");
                    if (avatarObj != null) avatar = String.valueOf(avatarObj);
                    // name/email no están garantizados aquí; quedan null excepto si la cuenta los provee
                    Object nameObj = accountMap.get("name");
                    if (nameObj != null) name = String.valueOf(nameObj);
                }
            }

            // Si no hay email, crear un fallback para respetar la restricción NOT NULL en la BD
            if (email == null && login != null) {
                email = login + "@users.noreply.github.com";
            }

            var userResp = userService.createOrUpdateFromGithub(ghId, login, email, installationId, avatar, name);
            return ResponseEntity.created(URI.create("/api/users/" + userResp.getId())).body(userResp);
        } catch (Exception e) {
            log.error("Error in registerWithInstallation: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
