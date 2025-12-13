package com.paradox.service_java.repository;

import com.paradox.service_java.model.GithubIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GithubIssueRepository extends JpaRepository<GithubIssue, UUID> {

    /**
     * Buscar issue por repo y número
     */
    Optional<GithubIssue> findByRepoIdAndNumber(UUID repoId, Integer number);

    /**
     * Buscar issue por githubIssueId
     */
    Optional<GithubIssue> findByGithubIssueId(Long githubIssueId);

    /**
     * Buscar issues por repo y estado
     */
    List<GithubIssue> findByRepoIdAndState(UUID repoId, String state);

    /**
     * Buscar issues por repo
     */
    List<GithubIssue> findByRepoId(UUID repoId);

    /**
     * Buscar issues por autor
     */
    List<GithubIssue> findByUserLogin(String userLogin);

    /**
     * Buscar issues que contengan un label específico
     */
    @Query(value = "SELECT i.* FROM github_issues i WHERE :label = ANY(i.labels)", nativeQuery = true)
    List<GithubIssue> findIssuesByLabel(@Param("label") String label);
    @Query(value = "SELECT * FROM github_issues WHERE :label = ANY(labels)", nativeQuery = true)
    List<GithubIssue> findByLabelsContaining(@Param("label") String label);

    /**
     * Buscar issues que contengan un assignee específico
     */
    @Query(value = "SELECT i.* FROM github_issues i WHERE :assignee = ANY(i.assignees)", nativeQuery = true)
    List<GithubIssue> findIssuesByAssignee(@Param("assignee") String assignee);
    @Query(value = "SELECT * FROM github_issues WHERE :assignee = ANY(assignees)", nativeQuery = true)
    List<GithubIssue> findByAssigneesContaining(@Param("assignee") String assignee);

    /**
     * Verificar si existe issue
     */
    boolean existsByRepoIdAndNumber(UUID repoId, Integer number);

    /**
     * Contar issues abiertos por repo
     */
    @Query("SELECT COUNT(gi) FROM GithubIssue gi WHERE gi.repo.id = :repoId AND gi.state = 'open'")
    Long countOpenIssuesByRepoId(@Param("repoId") UUID repoId);

    /**
     * Buscar issues por milestone
     */
    List<GithubIssue> findByMilestone(String milestone);

    /**
     * Buscar issues abiertas por repo y milestone
     */
    @Query("SELECT gi FROM GithubIssue gi WHERE gi.repo.id = :repoId AND gi.milestone = :milestone AND gi.state = 'open'")
    List<GithubIssue> findOpenIssuesByRepoIdAndMilestone(@Param("repoId") UUID repoId, @Param("milestone") String milestone);
}

