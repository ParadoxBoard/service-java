package com.paradox.service_java.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAllExceptions(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);

        ApiError e = new ApiError();
        e.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        e.setError(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        e.setMessage(ex.getMessage());
        e.setPath(req.getRequestURI());

        return new ResponseEntity<>(e, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(GithubAuthException.class)
    public ResponseEntity<ApiError> handleGithubAuthException(GithubAuthException ex, HttpServletRequest req) {
        log.error("GitHub authentication error: {}", ex.getMessage());

        ApiError e = new ApiError();
        e.setStatus(HttpStatus.UNAUTHORIZED.value());
        e.setError("GitHub Authentication Error");
        e.setMessage(ex.getMessage());
        e.setPath(req.getRequestURI());
        return new ResponseEntity<>(e, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.error("Data integrity violation: {}", ex.getMessage());

        ApiError e = new ApiError();
        e.setStatus(HttpStatus.CONFLICT.value());
        e.setError("Data Integrity Violation");
        e.setMessage("Duplicate entry or constraint violation");
        e.setPath(req.getRequestURI());
        return new ResponseEntity<>(e, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest req) {
        log.error("Validation error: {}", ex.getMessage());

        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ApiError e = new ApiError();
        e.setStatus(HttpStatus.BAD_REQUEST.value());
        e.setError("Validation Error");
        e.setMessage(errors);
        e.setPath(req.getRequestURI());
        return new ResponseEntity<>(e, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        log.error("Constraint violation: {}", ex.getMessage());

        String errors = ex.getConstraintViolations()
                .stream()
                .map((ConstraintViolation<?> cv) -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining(", "));

        ApiError e = new ApiError();
        e.setStatus(HttpStatus.BAD_REQUEST.value());
        e.setError("Constraint Violation");
        e.setMessage(errors);
        e.setPath(req.getRequestURI());
        return new ResponseEntity<>(e, HttpStatus.BAD_REQUEST);
    }
}
