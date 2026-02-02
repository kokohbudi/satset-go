package com.omnip.exceptions;

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
@RestControllerAdvice
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
     * Handle general runtime exceptions.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "error");
        response.put("code", "INTERNAL_ERROR");
        response.put("message", "Terjadi kesalahan server: " + ex.getMessage());

        log.error("Runtime exception", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
