package com.paradox.service_java.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para configuración de protección de branch
 * Responsabilidad: DEV B (Isabella)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchProtectionResponse {
    private String branchName;
    private Boolean isProtected;
    private Boolean requiresReview;
    private Integer requiredReviewers;
    private Boolean requiresStatusChecks;
    private Boolean requiresUpToDateBranch;
    private Boolean restrictsPushes;
    private Boolean allowsForcePushes;
    private Boolean allowsDeletions;
}

