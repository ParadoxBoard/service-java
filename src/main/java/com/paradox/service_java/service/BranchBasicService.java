package com.paradox.service_java.service;

import com.paradox.service_java.dto.BranchDetailResponse;
import com.paradox.service_java.dto.BranchResponse;
import com.paradox.service_java.dto.CommitResponse;
import com.paradox.service_java.model.Branch;
import com.paradox.service_java.model.Commit;
import com.paradox.service_java.repository.BranchRepository;
import com.paradox.service_java.repository.CommitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio ampliado para Branches (endpoints básicos)
 * DEV A (Adrian)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BranchBasicService {

    private final BranchRepository branchRepository;
    private final CommitRepository commitRepository;

    /**
     * Obtener todos los branches de un repositorio
     */
    @Transactional(readOnly = true)
    public List<BranchResponse> findByRepoId(UUID repoId) {
        log.info("Finding branches for repo: {}", repoId);

        List<Branch> branches = branchRepository.findByRepositoryIdOrderByNameAsc(repoId);

        return branches.stream()
                .map(this::toBranchResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtener detalles de un branch con commits recientes
     */
    @Transactional(readOnly = true)
    public Optional<BranchDetailResponse> findByIdWithCommits(UUID branchId) {
        log.info("Finding branch details for id: {}", branchId);

        Optional<Branch> branchOpt = branchRepository.findById(branchId);
        if (branchOpt.isEmpty()) {
            return Optional.empty();
        }

        Branch branch = branchOpt.get();

        // Obtener últimos 10 commits del branch
        List<Commit> recentCommits = commitRepository.findByBranchIdOrderByAuthorDateDesc(
                branchId, PageRequest.of(0, 10)
        ).getContent();

        // Contar total de commits
        Long totalCommits = commitRepository.countByBranchId(branchId);

        return Optional.of(toBranchDetailResponse(branch, recentCommits, totalCommits));
    }

    /**
     * Convierte Branch a BranchResponse
     */
    private BranchResponse toBranchResponse(Branch branch) {
        return BranchResponse.builder()
                .id(branch.getId())
                .name(branch.getName())
                .headSha(branch.getSha())
                .headMessage(branch.getCommitMessage())
                .headAuthor(branch.getCommitAuthor())
                .headDate(branch.getCommitDate())
                .isProtected(branch.getProtectedBranch())
                .createdAt(branch.getCreatedAt())
                .updatedAt(branch.getUpdatedAt())
                .repositoryId(branch.getRepository().getId())
                .repositoryName(branch.getRepository().getName())
                .build();
    }

    /**
     * Convierte Branch a BranchDetailResponse con commits
     */
    private BranchDetailResponse toBranchDetailResponse(Branch branch, List<Commit> recentCommits, Long totalCommits) {
        List<CommitResponse> commitResponses = recentCommits.stream()
                .map(this::toCommitResponse)
                .collect(Collectors.toList());

        return BranchDetailResponse.builder()
                .id(branch.getId())
                .name(branch.getName())
                .headSha(branch.getSha())
                .headMessage(branch.getCommitMessage())
                .headAuthor(branch.getCommitAuthor())
                .headDate(branch.getCommitDate())
                .isProtected(branch.getProtectedBranch())
                .createdAt(branch.getCreatedAt())
                .updatedAt(branch.getUpdatedAt())
                .repositoryId(branch.getRepository().getId())
                .repositoryName(branch.getRepository().getName())
                .repositoryFullName(branch.getRepository().getFullName())
                .totalCommitsCount(totalCommits)
                .recentCommits(commitResponses)
                .build();
    }

    /**
     * Convierte Commit a CommitResponse
     */
    private CommitResponse toCommitResponse(Commit commit) {
        return CommitResponse.builder()
                .id(commit.getId())
                .sha(commit.getSha())
                .message(commit.getMessage())
                .authorName(commit.getAuthorName())
                .authorEmail(commit.getAuthorEmail())
                .authorLogin(commit.getAuthorLogin())
                .authorDate(commit.getAuthorDate())
                .additions(commit.getAdditions())
                .deletions(commit.getDeletions())
                .changedFiles(commit.getChangedFiles())
                .htmlUrl(commit.getHtmlUrl())
                .createdAt(commit.getCreatedAt())
                .parents(commit.getParentShas())
                .branchId(commit.getBranch() != null ? commit.getBranch().getId() : null)
                .branchName(commit.getBranch() != null ? commit.getBranch().getName() : null)
                .repositoryId(commit.getRepository().getId())
                .repositoryName(commit.getRepository().getName())
                .build();
    }
}

