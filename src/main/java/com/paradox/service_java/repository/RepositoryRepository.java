package com.paradox.service_java.repository;

import com.paradox.service_java.model.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@org.springframework.stereotype.Repository
public interface RepositoryRepository extends JpaRepository<Repository, UUID> {

    Optional<Repository> findByGithubRepoId(Long githubRepoId);

    Optional<Repository> findByFullName(String fullName);

    List<Repository> findByInstallationId(UUID installationId);

    List<Repository> findByInstallation_InstallationId(Long installationId);

    List<Repository> findByOwnerLogin(String ownerLogin);

    boolean existsByGithubRepoId(Long githubRepoId);

    // Queries para estadísticas
    long countByPrivateRepo(Boolean privateRepo);

    long countByArchived(Boolean archived);
}

