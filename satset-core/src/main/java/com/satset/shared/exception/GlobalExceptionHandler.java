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
     * Build the standard error response body shared by all handlers.
     */
    private ResponseEntity<Map<String, Object>> body(String code, String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "error");
        response.put("code", code);
        response.put("message", message);
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Handle validation errors from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        // Collect all field errors
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Validation failed",
                        (first, second) -> first // Keep first message if duplicate field
                ));

        // Create summary message
        String message = fieldErrors.values().stream().findFirst().orElse("Validasi gagal");

        log.warn("Validation failed: {}", fieldErrors);

        ResponseEntity<Map<String, Object>> response = body("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST);
        response.getBody().put("errors", fieldErrors);
        return response;
    }

    /**
     * Handle business exceptions.
     */
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientBalance(InsufficientBalanceException ex) {
        log.warn("Insufficient balance: {}", ex.getErrorMessage());
        return body("INSUFFICIENT_BALANCE", ex.getErrorMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
        log.error("Business exception: {} - {}", ex.getErrorCode(), ex.getErrorMessage());
        HttpStatus status = "BILL_CHANGED".equals(ex.getErrorCode())
                ? HttpStatus.CONFLICT
                : HttpStatus.BAD_REQUEST;
        return body(ex.getErrorCode(), ex.getErrorMessage(), status);
    }

    /**
     * Kegagalan supplier eksternal (mis. Digiflazz rc 83 limit). Kode + pesan asli DF
     * diteruskan ke admin biar tahu status supplier; 502 Bad Gateway.
     */
    @ExceptionHandler(SupplierException.class)
    public ResponseEntity<Map<String, Object>> handleSupplierException(SupplierException ex) {
        log.error("Supplier error {}: {}", ex.getCode(), ex.getMessage());
        return body(ex.getCode(), ex.getMessage(), HttpStatus.BAD_GATEWAY);
    }

    /**
     * Handle resource not found exceptions (404).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        ResponseEntity<Map<String, Object>> response = body("NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND);
        response.getBody().put("resource", ex.getResourceName());
        return response;
    }

    /**
     * Handle general runtime exceptions.
     * Detail error hanya di-log, TIDAK dikirim ke client.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception: {}", ex.getMessage(), ex);
        return body("INTERNAL_ERROR", "Terjadi kesalahan server. Silakan coba lagi.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
