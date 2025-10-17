package com.aurionpro.papms.exception;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(Instant.now(), HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage(), req.getRequestURI()));
    }

    // --- MODIFICATION START ---
    // Handles custom exception for duplicate entities (e.g., username, company name)
    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<ApiError> handleDuplicate(DuplicateUserException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(Instant.now(), HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage(), req.getRequestURI()));
    }

    // Handles issues with business logic state (e.g., "invoice already paid", "token expired")
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(Instant.now(), HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage(), req.getRequestURI()));
    }

    // Handles invalid arguments passed to methods (e.g., "passwords do not match")
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage(), req.getRequestURI()));
    }

    // Handles authorization failures (e.g., "access denied", "you can only update your own profile")
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiError> handleSecurity(SecurityException ex, HttpServletRequest req) {
        log.warn("Security exception caught: {} for request {}", ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError(Instant.now(), HttpStatus.FORBIDDEN.value(), "Forbidden", ex.getMessage(), req.getRequestURI()));
    }
    // --- MODIFICATION END ---

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        // Simplified message for validation errors
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("Field '%s': %s", error.getField(), error.getDefaultMessage()))
                .findFirst()
                .orElse("Validation failed");

        return ResponseEntity.badRequest()
                .body(new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(), "Bad Request", message, req.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError()
                .body(new ApiError(Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", "An unexpected error occurred. Please contact support.", req.getRequestURI()));
    }
}