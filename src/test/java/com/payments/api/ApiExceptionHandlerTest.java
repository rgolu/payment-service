package com.payments.api;

import com.payments.api.dto.ErrorResponse;
import com.payments.domain.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void handleDomain_UsesExceptionStatus() {
        ResponseEntity<ErrorResponse> response = handler.handleDomain(new NotFoundException("missing"));
        assertEquals(404, response.getStatusCode().value());
        assertEquals("NotFoundException", response.getBody().error());
    }

    @Test
    void handleIllegal_Returns400() {
        ResponseEntity<ErrorResponse> response = handler.handleIllegal(new IllegalArgumentException("bad"));
        assertEquals(400, response.getStatusCode().value());
        assertEquals("IllegalArgumentException", response.getBody().error());
    }

    @Test
    void handleUnknown_Returns500() {
        ResponseEntity<ErrorResponse> response = handler.handleUnknown(new RuntimeException("boom"));
        assertEquals(500, response.getStatusCode().value());
        assertEquals("InternalError", response.getBody().error());
        assertEquals("boom", response.getBody().message());
    }
}
