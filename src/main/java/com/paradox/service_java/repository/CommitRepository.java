package com.paradox.service_java.repository;

import com.paradox.service_java.model.Commit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommitRepository extends JpaRepository<Commit, UUID> {

    /**
     * Buscar commit por SHA
     */
    Optional<Commit> findBySha(String sha);

    /**
     * Buscar commit por SHA y repositorio
     */
    Optional<Commit> findByRepositoryIdAndSha(UUID repositoryId, String sha);

    /**
     * Verificar si existe un commit por SHA en un repo
     */
    boolean existsByRepositoryIdAndSha(UUID repositoryId, String sha);

    /**
     * Obtener commits de un repositorio (paginado)
     */
    Page<Commit> findByRepositoryIdOrderByAuthorDateDesc(UUID repositoryId, Pageable pageable);

    /**
     * Obtener commits de un branch específico
     */
    Page<Commit> findByBranchIdOrderByAuthorDateDesc(UUID branchId, Pageable pageable);

    /**
     * Obtener commits por autor
     */
    List<Commit> findByAuthorLoginOrderByAuthorDateDesc(String authorLogin);

    Page<Commit> findByBranchIdAndAuthorLoginOrderByAuthorDateDesc(UUID branchId, String authorLogin, Pageable pageable);

    Page<Commit> findByBranchIdAndAuthorLoginAndAuthorDateBetweenOrderByAuthorDateDesc(
            UUID branchId, String authorLogin, OffsetDateTime from, OffsetDateTime to, Pageable pageable);

    Page<Commit> findByRepositoryIdAndAuthorLoginOrderByAuthorDateDesc(UUID repositoryId, String authorLogin, Pageable pageable);

    /**
     * Obtener commits recientes de un repo (últimos N)
     */
    @Query("SELECT c FROM Commit c WHERE c.repository.id = :repoId ORDER BY c.authorDate DESC")
    List<Commit> findRecentCommitsByRepo(@Param("repoId") UUID repoId, Pageable pageable);

    /**
     * Obtener commits entre fechas
     */
    @Query("SELECT c FROM Commit c WHERE c.repository.id = :repoId " +
           "AND c.authorDate BETWEEN :startDate AND :endDate " +
           "ORDER BY c.authorDate DESC")
    List<Commit> findByRepositoryAndDateRange(
            @Param("repoId") UUID repoId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    /**
     * Contar commits de un repositorio
     */
    long countByRepositoryId(UUID repositoryId);

    long countByRepository_Id(UUID repositoryId);

    Optional<com.paradox.service_java.model.Commit> findTopByRepository_IdOrderByAuthorDateDesc(UUID repositoryId);

    /**
     * Contar commits de un branch
     */
    long countByBranchId(UUID branchId);

    /**
     * Contar commits desde una fecha específica
     */
    @Query("SELECT COUNT(c) FROM Commit c WHERE c.authorDate >= :since")
    long countCommitsSince(@Param("since") OffsetDateTime since);
}

