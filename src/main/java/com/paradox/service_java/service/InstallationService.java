package com.paradox.service_java.service;

import com.paradox.service_java.model.Installation;
import com.paradox.service_java.repository.InstallationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio para gestionar instalaciones de GitHub App
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstallationService {

    private final InstallationRepository installationRepository;

    /**
     * Crea o actualiza una instalación desde los datos de GitHub
     */
    @Transactional
    public Installation createOrUpdateFromGitHub(Map<String, Object> installationData) {
        Long installationId = extractLong(installationData, "id");

        if (installationId == null) {
            throw new IllegalArgumentException("Installation ID is required");
        }

        Optional<Installation> existing = installationRepository.findByInstallationId(installationId);

        Installation installation;
        if (existing.isPresent()) {
            installation = existing.get();
            log.info("Updating existing installation: {}", installationId);
        } else {
            installation = new Installation();
            installation.setInstallationId(installationId);
            log.info("Creating new installation: {}", installationId);
        }

        // Extraer datos de account
        Map<String, Object> account = extractMap(installationData, "account");
        if (account != null) {
            installation.setAccountLogin(extractString(account, "login"));
            installation.setAccountType(extractString(account, "type"));
            installation.setAccountId(extractLong(account, "id"));
        }

        // Otros campos
        installation.setTargetType(extractString(installationData, "target_type"));
        installation.setRepositorySelection(extractString(installationData, "repository_selection"));
        installation.setAppId(extractLong(installationData, "app_id"));
        installation.setAppSlug(extractString(installationData, "app_slug"));

        // Permisos y eventos
        Map<String, Object> permissions = extractMap(installationData, "permissions");
        if (permissions != null) {
            installation.setPermissions(permissions);
        }

        List<String> events = extractStringList(installationData, "events");
        if (events != null) {
            installation.setEvents(events);
        }

        return installationRepository.save(installation);
    }

    /**
     * Busca una instalación por su ID de GitHub
     */
    public Optional<Installation> findByInstallationId(Long installationId) {
        return installationRepository.findByInstallationId(installationId);
    }

    /**
     * Busca una instalación por el login de la cuenta
     */
    public Optional<Installation> findByAccountLogin(String accountLogin) {
        return installationRepository.findByAccountLogin(accountLogin);
    }

    /**
     * Marca una instalación como suspendida
     */
    @Transactional
    public void suspend(Long installationId) {
        installationRepository.findByInstallationId(installationId).ifPresent(installation -> {
            installation.setSuspendedAt(OffsetDateTime.now());
            installationRepository.save(installation);
            log.info("Installation suspended: {}", installationId);
        });
    }

    /**
     * Elimina una instalación
     */
    @Transactional
    public void delete(Long installationId) {
        installationRepository.findByInstallationId(installationId).ifPresent(installation -> {
            installationRepository.delete(installation);
            log.info("Installation deleted: {}", installationId);
        });
    }

    // ===== Métodos auxiliares de extracción =====

    private String extractString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    private Long extractLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            log.warn("Could not parse long from key {}: {}", key, value);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractStringList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return null;
    }
}

