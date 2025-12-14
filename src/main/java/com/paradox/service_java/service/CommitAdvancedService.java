package com.paradox.service_java.service;

import com.paradox.service_java.dto.CommitFileResponse;
import com.paradox.service_java.model.Commit;
import com.paradox.service_java.repository.CommitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio avanzado para commits
 * Responsabilidad: DEV B (Isabella)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommitAdvancedService {

    private final CommitRepository commitRepository;
    private final GitHubApiService gitHubApiService;
    private final InstallationTokenService installationTokenService;

    /**
     * Obtener commits de un branch específico por nombre
     */
    public List<Commit> getCommitsByBranchName(UUID repoId, String branchName, int limit) {
        log.info("Getting commits for repo: {} and branch: {}", repoId, branchName);

        // Por ahora retornamos commits del repo ordenados por fecha
        // En una implementación completa, filtrarías por branchName
        Pageable pageable = PageRequest.of(0, limit);
        return commitRepository.findRecentCommitsByRepo(repoId, pageable);
    }

    /**
     * Obtener archivos modificados en un commit desde GitHub API
     */
    public List<CommitFileResponse> getCommitFiles(String sha, Long installationId, String owner, String repoName) {
        log.info("Getting files for commit: {} in repo: {}/{}", sha, owner, repoName);

        try {
            String token = installationTokenService.getInstallationToken(installationId);
            Map<String, Object> commitData = gitHubApiService.getCommitFiles(owner, repoName, sha, token);

            if (!commitData.containsKey("files")) {
                return List.of();
            }

            List<Map<String, Object>> files = (List<Map<String, Object>>) commitData.get("files");

            return files.stream()
                    .map(file -> CommitFileResponse.builder()
                            .filename((String) file.get("filename"))
                            .status((String) file.get("status"))
                            .additions(file.containsKey("additions") ? ((Number) file.get("additions")).intValue() : 0)
                            .deletions(file.containsKey("deletions") ? ((Number) file.get("deletions")).intValue() : 0)
                            .changes(file.containsKey("changes") ? ((Number) file.get("changes")).intValue() : 0)
                            .blobUrl((String) file.get("blob_url"))
                            .rawUrl((String) file.get("raw_url"))
                            .patch(file.containsKey("patch") ? (String) file.get("patch") : null)
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching commit files from GitHub API: {}", e.getMessage());
            throw new RuntimeException("Could not fetch commit files: " + e.getMessage());
        }
    }
}

