package com.paradox.service_java.service;

import com.paradox.service_java.model.Branch;
import com.paradox.service_java.model.Commit;
import com.paradox.service_java.model.Repository;
import com.paradox.service_java.repository.CommitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio para gestionar commits
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommitService {

    private final CommitRepository commitRepository;

    /**
     * Crear commit si no existe (verificar por SHA)
     */
    @Transactional
    public Commit createIfNotExists(Repository repository, Branch branch,
                                     String sha, String message,
                                     String authorName, String authorEmail, String authorLogin,
                                     OffsetDateTime authorDate,
                                     String treeSha, List<String> parentShas,
                                     Integer additions, Integer deletions, Integer changedFiles,
                                     String htmlUrl) {

        // Verificar si ya existe el commit
        Optional<Commit> existing = commitRepository.findByRepositoryIdAndSha(repository.getId(), sha);

        if (existing.isPresent()) {
            log.debug("Commit already exists: {}", sha);
            return existing.get();
        }

        // Crear nuevo commit
        Commit commit = Commit.builder()
                .repository(repository)
                .branch(branch)
                .sha(sha)
                .message(message)
                .authorName(authorName)
                .authorEmail(authorEmail)
                .authorLogin(authorLogin)
                .authorDate(authorDate)
                .committerName(authorName) // Por defecto, committer = author
                .committerEmail(authorEmail)
                .committerDate(authorDate)
                .treeSha(treeSha)
                .parentShas(parentShas)
                .additions(additions)
                .deletions(deletions)
                .changedFiles(changedFiles)
                .htmlUrl(htmlUrl)
                .verified(false)
                .build();

        Commit saved = commitRepository.save(commit);
        log.info("Commit created: {} in repo: {} (branch: {})",
                sha.substring(0, 7), repository.getFullName(), branch.getName());

        return saved;
    }

    /**
     * Verificar si un commit ya existe
     */
    public boolean existsBySha(String sha) {
        return commitRepository.findBySha(sha).isPresent();
    }

    /**
     * Buscar commit por SHA
     */
    public Optional<Commit> findBySha(String sha) {
        return commitRepository.findBySha(sha);
    }
}

