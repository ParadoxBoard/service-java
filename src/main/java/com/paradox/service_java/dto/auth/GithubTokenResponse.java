package com.paradox.service_java.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response de GitHub al intercambiar el código por access token
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GithubTokenResponse {
    
    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("token_type")
    private String tokenType;
    
    private String scope;
    
    private String error;
    
    @JsonProperty("error_description")
    private String errorDescription;
}

