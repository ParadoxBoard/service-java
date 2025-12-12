package com.paradox.service_java.service;

import com.paradox.service_java.dto.UserRequest;
import com.paradox.service_java.dto.UserResponse;
import com.paradox.service_java.model.User;
import com.paradox.service_java.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse createUser(UserRequest request) {
        User u = new User();
        u.setEmail(request.getEmail());
        u.setUsername(request.getUsername());
        u.setName(request.getName());
        u.setAvatarUrl(request.getAvatarUrl());
        if (request.getPassword() != null) {
            // placeholder: hash password (implement BCrypt in production)
            u.setPasswordHash(request.getPassword());
        }
        User saved;
        try {
            saved = userRepository.save(u);
        } catch (DataIntegrityViolationException ex) {
            throw ex;
        }
        return toResponse(saved);
    }

    @Transactional
    public UserResponse createOrUpdateFromGithub(Long githubId, String login, String email, Long installationId, String avatarUrl, String name) {
        // buscar por githubId o email
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            // intento por github id (si se guarda como string)
            // buscar por username igual al login
            user = userRepository.findByUsername(login).orElse(null);
        }

        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setUsername(login);
            user.setName(name);
            user.setAvatarUrl(avatarUrl);
            // set github-specific fields
            user.setGithubId(String.valueOf(githubId));
            user.setGithubInstallationId(String.valueOf(installationId));
            user = userRepository.save(user);
        } else {
            // update fields
            user.setGithubId(String.valueOf(githubId));
            user.setGithubInstallationId(String.valueOf(installationId));
            user.setAvatarUrl(avatarUrl);
            user.setName(name);
            user = userRepository.save(user);
        }
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserById(UUID id) {
        return userRepository.findById(id).map(this::toResponse);
    }

    private UserResponse toResponse(User u) {
        UserResponse r = new UserResponse();
        r.setId(u.getId());
        r.setEmail(u.getEmail());
        r.setUsername(u.getUsername());
        r.setName(u.getName());
        r.setAvatarUrl(u.getAvatarUrl());
        r.setCreatedAt(u.getCreatedAt());
        r.setLastSeenAt(u.getLastSeenAt());
        return r;
    }
}
