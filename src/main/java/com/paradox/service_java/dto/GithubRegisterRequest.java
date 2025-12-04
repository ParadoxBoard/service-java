package com.paradox.service_java.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GithubRegisterRequest {
    @NotNull
    private Long installationId;
}

