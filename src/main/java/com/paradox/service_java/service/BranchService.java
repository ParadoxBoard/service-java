package com.paradox.service_java.service;

import com.paradox.service_java.model.Branch;
import com.paradox.service_java.model.Repository;
import com.paradox.service_java.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio para gestionar branches de repositorios
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;

    /**
     * Crear o actualizar un branch
     */
    @Transactional
    public Branch createOrUpdate(Repository repository, String branchName, String sha,
                                  String commitMessage, String commitAuthor, OffsetDateTime commitDate) {

        Optional<Branch> existingBranch = branchRepository.findByRepositoryIdAndName(
                repository.getId(), branchName);

        Branch branch;
        if (existingBranch.isPresent()) {
            branch = existingBranch.get();
            log.debug("Updating existing branch: {} in repo: {}", branchName, repository.getFullName());
        } else {
            branch = Branch.builder()
                    .repository(repository)
                    .name(branchName)
                    .build();
            log.info("Creating new branch: {} in repo: {}", branchName, repository.getFullName());
        }

        // Actualizar datos
        branch.setSha(sha);
        branch.setCommitMessage(commitMessage);
        branch.setCommitAuthor(commitAuthor);
        branch.setCommitDate(commitDate);

        return branchRepository.save(branch);
    }

    /**
     * Buscar branch por repositorio y nombre
     */
    public Optional<Branch> findByRepositoryAndName(UUID repositoryId, String branchName) {
        return branchRepository.findByRepositoryIdAndName(repositoryId, branchName);
    }

    /**
     * Marcar branch como eliminado
     */
    @Transactional
    public void deleteBranch(UUID repositoryId, String branchName) {
        Optional<Branch> branch = branchRepository.findByRepositoryIdAndName(repositoryId, branchName);
        branch.ifPresent(b -> {
            branchRepository.delete(b);
            log.info("Branch deleted: {} from repo: {}", branchName, repositoryId);
        });
    }
}

