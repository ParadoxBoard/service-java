package com.paradox.service_java.repository;

import com.paradox.service_java.model.Installation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstallationRepository extends JpaRepository<Installation, UUID> {

    Optional<Installation> findByInstallationId(Long installationId);

    Optional<Installation> findByAccountLogin(String accountLogin);

    boolean existsByInstallationId(Long installationId);

    Optional<Installation> findFirstByOrderByCreatedAtDesc();
}

