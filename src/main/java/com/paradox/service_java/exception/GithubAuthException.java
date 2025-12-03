package com.paradox.service_java.exception;

/**
 * Excepción personalizada para errores de autenticación con GitHub
 */
public class GithubAuthException extends RuntimeException {
    
    public GithubAuthException(String message) {
        super(message);
    }
    
    public GithubAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}

