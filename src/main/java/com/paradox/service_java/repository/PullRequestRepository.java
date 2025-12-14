package com.paradox.service_java.repository;

import com.paradox.service_java.model.PullRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PullRequestRepository extends JpaRepository<PullRequest, UUID> {

    /**
     * Buscar PR por repo y número
     */
    Optional<PullRequest> findByRepoIdAndNumber(UUID repoId, Integer number);

    /**
     * Buscar PR por githubPrId
     */
    Optional<PullRequest> findByGithubPrId(Long githubPrId);

    /**
     * Buscar PRs por repo y estado
     */
    List<PullRequest> findByRepoIdAndState(UUID repoId, String state);

    /**
     * Buscar PRs abiertos por repo
     */
    @Query("SELECT pr FROM PullRequest pr WHERE pr.repo.id = :repoId AND pr.state = 'open'")
    List<PullRequest> findOpenPRsByRepoId(@Param("repoId") UUID repoId);

    /**
     * Buscar PRs por autor
     */
    List<PullRequest> findByUserLogin(String userLogin);

    /**
     * Buscar PRs por repo
     */
    List<PullRequest> findByRepoId(UUID repoId);

    Page<PullRequest> findByRepoId(UUID repoId, Pageable pageable);

    Page<PullRequest> findByRepoIdAndState(UUID repoId, String state, Pageable pageable);

    Page<PullRequest> findByRepoIdAndUserLogin(UUID repoId, String userLogin, Pageable pageable);

    Page<PullRequest> findByRepoIdAndStateAndUserLogin(UUID repoId, String state, String userLogin, Pageable pageable);

    /**
     * Verificar si existe PR
     */
    boolean existsByRepoIdAndNumber(UUID repoId, Integer number);

    /**
     * Contar PRs abiertos por repo
     */
    @Query("SELECT COUNT(pr) FROM PullRequest pr WHERE pr.repo.id = :repoId AND pr.state = 'open'")
    Long countOpenPRsByRepoId(@Param("repoId") UUID repoId);

    Long countByRepoIdAndState(UUID repoId, String state);

    /**
     * Buscar PRs mergeados por repo
     */
    @Query("SELECT pr FROM PullRequest pr WHERE pr.repo.id = :repoId AND pr.merged = true")
    List<PullRequest> findMergedPRsByRepoId(@Param("repoId") UUID repoId);
}

