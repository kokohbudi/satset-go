package com.satset.shared.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST API errors.
 * Handles validation errors and business exceptions.
 */
@Slf4j
@RestControllerAdvice(annotations = org.springframework.web.bind.annotation.RestController.class)
public class GlobalExceptionHandler {

    /**
     * Handle validation errors from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "error");
        response.put("code", "VALIDATION_ERROR");

        // Collect all field errors
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Validation failed",
                        (first, second) -> first // Keep first message if duplicate field
                ));

        response.put("errors", fieldErrors);

        // Create summary message
        String message = fieldErrors.values().stream().findFirst().orElse("Validasi gagal");
        response.put("message", message);

        log.warn("Validation failed: {}", fieldErrors);

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle business exceptions.
     */
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientBalance(InsufficientBalanceException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "error");
        response.put("code", "INSUFFICIENT_BALANCE");
        response.put("message", ex.getErrorMessage());

        log.warn("Insufficient balance: {}", ex.getErrorMessage());

        return ResponseEntity.status(422).body(response);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "error");
        response.put("code", ex.getErrorCode());
        response.put("message", ex.getErrorMessage());

        log.error("Business exception: {} - {}", ex.getErrorCode(), ex.getErrorMessage());

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle resource not found exceptions (404).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "error");
        response.put("code", "NOT_FOUND");
        response.put("message", ex.getMessage());
        response.put("resource", ex.getResourceName());

        log.warn("Resource not found: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle general runtime exceptions.
     * Detail error hanya di-log, TIDAK dikirim ke client.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "error");
        response.put("code", "INTERNAL_ERROR");
        response.put("message", "Terjadi kesalahan server. Silakan coba lagi.");

        log.error("Runtime exception: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
