package com.paradox.service_java.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa los datos del usuario obtenidos de la API de GitHub
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GithubUserDTO {
    
    private Long id;
    
    private String login;
    
    private String email;
    
    private String name;
    
    @JsonProperty("avatar_url")
    private String avatarUrl;
    
    @JsonProperty("html_url")
    private String htmlUrl;
    
    private String bio;
    
    private String company;
    
    private String location;
}

