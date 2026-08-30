package com.nexora.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class NexoraException extends RuntimeException {

    private final int statusCode;
    private final String code;

    public NexoraException(String message) {
        this(message, 400, defaultCode(400));
    }

    public NexoraException(String message, int statusCode) {
        this(message, statusCode, defaultCode(statusCode));
    }

    public NexoraException(String message, int statusCode, String code) {
        super(message);
        this.statusCode = statusCode;
        this.code = code;
    }

    public NexoraException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 400;
        this.code = defaultCode(400);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getCode() {
        return code;
    }

    private static String defaultCode(int status) {
        return switch (status) {
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 409 -> "CONFLICT";
            case 502 -> "UPSTREAM_ERROR";
            default -> "REQUEST_FAILED";
        };
    }
}
