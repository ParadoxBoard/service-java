package com.paradox.service_java.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAll(Exception ex, HttpServletRequest req) {
        ApiError e = new ApiError();
        e.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        e.setError(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        e.setMessage(ex.getMessage());
        e.setPath(req.getRequestURI());
        return new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}

