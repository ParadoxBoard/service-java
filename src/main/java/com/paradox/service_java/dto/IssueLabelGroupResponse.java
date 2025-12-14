package com.paradox.service_java.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para agrupar issues por label
 * Responsabilidad: DEV B (Isabella)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueLabelGroupResponse {
    private String label;
    private Integer totalIssues;
    private Integer openIssues;
    private Integer closedIssues;
    private List<IssueSimpleResponse> issues;
}

