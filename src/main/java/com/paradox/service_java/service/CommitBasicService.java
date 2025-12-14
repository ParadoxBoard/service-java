package com.paradox.service_java.service;

import com.paradox.service_java.dto.CommitDetailResponse;
import com.paradox.service_java.dto.CommitResponse;
import com.paradox.service_java.dto.PaginatedResponse;
import com.paradox.service_java.model.Commit;
import com.paradox.service_java.repository.CommitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio ampliado para Commits (endpoints básicos)
 * DEV A (Adrian)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommitBasicService {

    private final CommitRepository commitRepository;

    /**
     * Buscar commits con filtros y paginación
     */
    @Transactional(readOnly = true)
    public PaginatedResponse<CommitResponse> findByFilters(
            UUID repoId,
            UUID branchId,
            String author,
            OffsetDateTime from,
            OffsetDateTime to,
            int page,
            int size) {

        log.info("Finding commits - repo: {}, branch: {}, author: {}, from: {}, to: {}, page: {}, size: {}",
                repoId, branchId, author, from, to, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<Commit> commitPage;

        // Aplicar filtros según lo que esté presente
        if (branchId != null && author != null && from != null && to != null) {
            commitPage = commitRepository.findByBranchIdAndAuthorLoginAndAuthorDateBetweenOrderByAuthorDateDesc(
                    branchId, author, from, to, pageable);
        } else if (branchId != null && author != null) {
            commitPage = commitRepository.findByBranchIdAndAuthorLoginOrderByAuthorDateDesc(
                    branchId, author, pageable);
        } else if (branchId != null) {
            commitPage = commitRepository.findByBranchIdOrderByAuthorDateDesc(branchId, pageable);
        } else if (repoId != null && author != null) {
            commitPage = commitRepository.findByRepositoryIdAndAuthorLoginOrderByAuthorDateDesc(
                    repoId, author, pageable);
        } else if (repoId != null) {
            commitPage = commitRepository.findByRepositoryIdOrderByAuthorDateDesc(repoId, pageable);
        } else {
            log.warn("No filters provided, returning empty result");
            commitPage = Page.empty();
        }

        List<CommitResponse> content = commitPage.getContent().stream()
                .map(this::toCommitResponse)
                .collect(Collectors.toList());

        return PaginatedResponse.of(content, page, size, commitPage.getTotalElements());
    }

    /**
     * Buscar commit por SHA con detalles
     */
    @Transactional(readOnly = true)
    public Optional<CommitDetailResponse> findByShaWithDetails(String sha) {
        log.info("Finding commit details for SHA: {}", sha);

        Optional<Commit> commitOpt = commitRepository.findBySha(sha);
        if (commitOpt.isEmpty()) {
            return Optional.empty();
        }

        Commit commit = commitOpt.get();
        return Optional.of(toCommitDetailResponse(commit));
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

    /**
     * Convierte Commit a CommitDetailResponse
     */
    private CommitDetailResponse toCommitDetailResponse(Commit commit) {
        return CommitDetailResponse.builder()
                .id(commit.getId())
                .sha(commit.getSha())
                .message(commit.getMessage())
                .treeSha(commit.getTreeSha())
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
                .repositoryFullName(commit.getRepository().getFullName())
                .build();
    }
}

