package com.satset.shared.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ==================== handleInsufficientBalance ====================

    @Test
    void handleInsufficientBalance_Returns422WithCode() {
        InsufficientBalanceException ex = new InsufficientBalanceException("Saldo tidak cukup");

        ResponseEntity<Map<String, Object>> response = handler.handleInsufficientBalance(ex);

        assertEquals(422, response.getStatusCode().value());
        assertEquals("error", response.getBody().get("status"));
        assertEquals("INSUFFICIENT_BALANCE", response.getBody().get("code"));
        assertEquals("Saldo tidak cukup", response.getBody().get("message"));
    }

    // ==================== handleBusinessException ====================

    @Test
    void handleBusinessException_Returns400WithErrorCode() {
        BusinessException ex = new BusinessException("EMAIL_TAKEN", "Email sudah dipakai");

        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(ex);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("error", response.getBody().get("status"));
        assertEquals("EMAIL_TAKEN", response.getBody().get("code"));
        assertEquals("Email sudah dipakai", response.getBody().get("message"));
    }

    // ==================== handleResourceNotFound ====================

    @Test
    void handleResourceNotFound_Returns404WithResource() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Store", "abc-123");

        ResponseEntity<Map<String, Object>> response = handler.handleResourceNotFound(ex);

        assertEquals(404, response.getStatusCode().value());
        assertEquals("error", response.getBody().get("status"));
        assertEquals("NOT_FOUND", response.getBody().get("code"));
        assertEquals("Store", response.getBody().get("resource"));
    }

    // ==================== handleRuntimeException ====================

    @Test
    void handleRuntimeException_Returns500WithGenericMessage() {
        RuntimeException ex = new RuntimeException("internal DB error");

        ResponseEntity<Map<String, Object>> response = handler.handleRuntimeException(ex);

        assertEquals(500, response.getStatusCode().value());
        assertEquals("error", response.getBody().get("status"));
        assertEquals("INTERNAL_ERROR", response.getBody().get("code"));
        // Should NOT expose internal error message
        assertNotEquals("internal DB error", response.getBody().get("message"));
    }

    // ==================== handleValidationExceptions (via MockMvc) ====================

    @Test
    void handleValidationExceptions_Returns400WithFieldErrors() throws Exception {
        // Setup a controller with @Valid
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(handler)
                .build();

        mockMvc.perform(post("/test/validate")
                        .contentType("application/json")
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").exists());
    }

    // ==================== Stub controller for validation test ====================

    @RestController
    static class TestController {

        static class Req {
            @NotBlank(message = "Name is required")
            public String name;
        }

        @PostMapping("/test/validate")
        public ResponseEntity<String> validate(@Valid @RequestBody Req req) {
            return ResponseEntity.ok("ok");
        }
    }
}
