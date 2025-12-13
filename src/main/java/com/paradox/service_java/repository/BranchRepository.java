package com.paradox.service_java.repository;

import com.paradox.service_java.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BranchRepository extends JpaRepository<Branch, UUID> {

    /**
     * Buscar branch por nombre y repositorio
     */
    Optional<Branch> findByRepositoryIdAndName(UUID repositoryId, String name);

    /**
     * Verificar si existe un branch
     */
    boolean existsByRepositoryIdAndName(UUID repositoryId, String name);

    /**
     * Obtener todos los branches de un repositorio
     */
    List<Branch> findByRepositoryIdOrderByNameAsc(UUID repositoryId);

    /**
     * Obtener branches protegidos de un repo
     */
    List<Branch> findByRepositoryIdAndProtectedBranchTrue(UUID repositoryId);

    /**
     * Buscar branch por SHA
     */
    Optional<Branch> findByRepositoryIdAndSha(UUID repositoryId, String sha);

    /**
     * Obtener branches actualizados recientemente
     */
    @Query("SELECT b FROM Branch b WHERE b.repository.id = :repoId " +
           "AND b.updatedAt > :since ORDER BY b.updatedAt DESC")
    List<Branch> findRecentlyUpdatedBranches(
            @Param("repoId") UUID repoId,
            @Param("since") OffsetDateTime since
    );

    /**
     * Contar branches de un repositorio
     */
    long countByRepositoryId(UUID repositoryId);

    /**
     * Obtener branch por defecto (main o master)
     */
    @Query("SELECT b FROM Branch b WHERE b.repository.id = :repoId " +
           "AND (b.name = 'main' OR b.name = 'master') " +
           "ORDER BY CASE WHEN b.name = 'main' THEN 0 ELSE 1 END")
    Optional<Branch> findDefaultBranch(@Param("repoId") UUID repoId);
}

