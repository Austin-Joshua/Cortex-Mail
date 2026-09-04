package com.nexora.exception;

import lombok.extern.slf4j.Slf4j;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NexoraException.class)
    public ResponseEntity<Map<String, Object>> handleNexoraException(NexoraException ex) {
        String requestId = newRequestId();
        log.error("NexoraException requestId={} status={} code={}: {}",
                requestId, ex.getStatusCode(), ex.getCode(), ex.getMessage());
        return buildError(clientSafeMessage(ex.getMessage(), ex.getStatusCode()),
                ex.getStatusCode(), ex.getCode(), requestId);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        String requestId = newRequestId();
        int status = ex.getStatusCode().value();
        log.error("ResponseStatusException requestId={} status={}: {}", requestId, status, ex.getReason());
        return buildError(clientSafeMessage(ex.getReason(), status), status, defaultCode(status), requestId);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return buildError("Access denied", 403, "FORBIDDEN", newRequestId());
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<Map<String, Object>> handleRateLimited(RequestNotPermitted ex) {
        return buildError(
                "You have reached the query limit of 20 per hour. Please try again later.",
                429,
                "RATE_LIMITED",
                newRequestId());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        String requestId = newRequestId();
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", 400);
        body.put("code", "VALIDATION_FAILED");
        body.put("message", "Validation failed");
        body.put("error", "Validation failed");
        body.put("requestId", requestId);
        body.put("details", errors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        String requestId = newRequestId();
        log.error("Unexpected error requestId={}: {}", requestId, ex.getMessage(), ex);
        return buildError("An unexpected error occurred. Please try again.", 500, "INTERNAL_ERROR", requestId);
    }

    private ResponseEntity<Map<String, Object>> buildError(String message, int status, String code, String requestId) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status);
        body.put("code", code);
        body.put("message", message);
        body.put("error", message);
        body.put("requestId", requestId);
        return ResponseEntity.status(status).body(body);
    }

    private static String newRequestId() {
        return UUID.randomUUID().toString();
    }

    private static String defaultCode(int status) {
        return switch (status) {
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 409 -> "CONFLICT";
            case 429 -> "RATE_LIMITED";
            case 502 -> "UPSTREAM_ERROR";
            default -> "REQUEST_FAILED";
        };
    }

    /** Keep Google/SQL/class names out of API responses. */
    private static String clientSafeMessage(String message, int status) {
        if (message == null || message.isBlank()) {
            return status >= 500 ? "An unexpected error occurred. Please try again." : "Request failed";
        }
        String lower = message.toLowerCase();
        if (lower.contains("sql") || lower.contains("hibernate") || lower.contains("jdbc")
                || lower.contains("stack") || lower.contains("exception")
                || lower.contains("token") || lower.contains("secret")) {
            return status >= 500 ? "An unexpected error occurred. Please try again." : "Request failed";
        }
        return message;
    }
}
