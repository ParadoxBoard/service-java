package com.paradox.service_java.service.auth;

import com.paradox.service_java.dto.auth.AuthResponse;
import com.paradox.service_java.dto.auth.GithubUserDTO;
import com.paradox.service_java.model.User;
import com.paradox.service_java.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Servicio principal de autenticación que coordina el flujo de login con GitHub
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final GithubOAuthService githubOAuthService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    /**
     * Maneja el callback de GitHub OAuth y realiza el login/registro del usuario
     */
    @Transactional
    public AuthResponse handleGithubCallback(String code) {
        log.info("Processing GitHub OAuth callback");

        // 1. Intercambiar código por access token
        String accessToken = githubOAuthService.exchangeCodeForAccessToken(code);

        // 2. Obtener datos del usuario de GitHub
        GithubUserDTO githubUser = githubOAuthService.fetchUserData(accessToken);

        // 3. Buscar o crear usuario en nuestra BD
        User user = findOrCreateUser(githubUser);

        // 4. Actualizar last_seen_at
        user.setLastSeenAt(OffsetDateTime.now());
        userRepository.save(user);

        // 5. Generar JWT interno
        String jwt = jwtService.generateToken(
                user.getId(),
                user.getUsername(),
                user.getEmail()
        );

        log.info("Successfully authenticated user: {}", user.getUsername());

        // 6. Construir respuesta
        return AuthResponse.builder()
                .token(jwt)
                .type("Bearer")
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .username(user.getUsername())
                        .name(user.getName())
                        .avatarUrl(user.getAvatarUrl())
                        .createdAt(user.getCreatedAt())
                        .build())
                .build();
    }

    /**
     * Busca un usuario existente por username o email, o crea uno nuevo
     */
    private User findOrCreateUser(GithubUserDTO githubUser) {
        // Intentar buscar por username (login de GitHub)
        Optional<User> existingUser = userRepository.findByUsername(githubUser.getLogin());

        if (existingUser.isPresent()) {
            log.info("Found existing user by username: {}", githubUser.getLogin());
            User user = existingUser.get();
            
            // Actualizar datos del usuario con la info más reciente de GitHub
            updateUserFromGithub(user, githubUser);
            return user;
        }

        // Si no existe, intentar buscar por email (si GitHub lo proporciona)
        if (githubUser.getEmail() != null) {
            existingUser = userRepository.findByEmail(githubUser.getEmail());
            if (existingUser.isPresent()) {
                log.info("Found existing user by email: {}", githubUser.getEmail());
                User user = existingUser.get();
                updateUserFromGithub(user, githubUser);
                return user;
            }
        }

        // Si no existe, crear nuevo usuario
        log.info("Creating new user from GitHub: {}", githubUser.getLogin());
        return createUserFromGithub(githubUser);
    }

    /**
     * Crea un nuevo usuario desde los datos de GitHub
     */
    private User createUserFromGithub(GithubUserDTO githubUser) {
        User user = new User();
        user.setUsername(githubUser.getLogin());
        user.setEmail(githubUser.getEmail());
        user.setName(githubUser.getName());
        user.setAvatarUrl(githubUser.getAvatarUrl());
        user.setCreatedAt(OffsetDateTime.now());
        
        return userRepository.save(user);
    }

    /**
     * Actualiza los datos del usuario con la información más reciente de GitHub
     */
    private void updateUserFromGithub(User user, GithubUserDTO githubUser) {
        boolean updated = false;

        if (githubUser.getId() != null) {
            String githubId = String.valueOf(githubUser.getId());
            if (!githubId.equals(user.getGithubId())) {
                user.setGithubId(githubId);
                updated = true;
            }
        }

        if (githubUser.getEmail() != null && !githubUser.getEmail().equals(user.getEmail())) {
            user.setEmail(githubUser.getEmail());
            updated = true;
        }

        if (githubUser.getName() != null && !githubUser.getName().equals(user.getName())) {
            user.setName(githubUser.getName());
            updated = true;
        }

        if (githubUser.getAvatarUrl() != null && !githubUser.getAvatarUrl().equals(user.getAvatarUrl())) {
            user.setAvatarUrl(githubUser.getAvatarUrl());
            updated = true;
        }

        if (updated) {
            log.info("Updated user data from GitHub for user: {}", user.getUsername());
            userRepository.save(user);
        }
    }
}

