package com.paradox.service_java.dto;
}
    private Integer issuesOpen;
    private Integer pullRequestsOpen;
    private Integer commitsLast30Days;
    private Map<String, Integer> reposByLanguage;
    private Integer activeRepos;
    private Integer archivedRepos;
    private Integer privateRepos;
    private Integer publicRepos;
    private Integer totalRepos;
public class RepoStatsResponse {
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
 */
 * Responsabilidad: DEV B (Isabella)
 * DTO para estadísticas de repositorios
/**

import java.util.Map;

import lombok.NoArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;


