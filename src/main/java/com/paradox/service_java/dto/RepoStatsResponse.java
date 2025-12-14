package com.paradox.service_java.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO para estadísticas de repositorios
 * Responsabilidad: DEV B (Isabella)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepoStatsResponse {
    private Integer totalRepos;
    private Integer publicRepos;
    private Integer privateRepos;
    private Integer archivedRepos;
    private Integer activeRepos;
    private Map<String, Integer> reposByLanguage;
    private Integer commitsLast30Days;
    private Integer pullRequestsOpen;
    private Integer issuesOpen;
}

