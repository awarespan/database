package com.sip.backend.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    private ResponseEntity<ApiError> handle(HttpStatus status, String code, String message,
                                            HttpServletRequest req, String requestId) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), code, message, req.getRequestURI(), requestId));
    }

    private String requestId(HttpServletRequest req) {
        return (String) req.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> badCredentials(BadCredentialsException ex, HttpServletRequest req) {
        return handle(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS",
                "Invalid username or password", req, requestId(req));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> authentication(AuthenticationException ex, HttpServletRequest req) {
        return handle(HttpStatus.UNAUTHORIZED, "AUTH_SESSION_EXPIRED",
                "Authentication required", req, requestId(req));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return handle(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN",
                "You do not have permission to perform this action", req, requestId(req));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return handle(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, req, requestId(req));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> illegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return handle(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), req, requestId(req));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> notFound(ResourceNotFoundException ex, HttpServletRequest req) {
        String code = ex.getCode() != null ? ex.getCode() : "NOT_FOUND";
        return handle(HttpStatus.NOT_FOUND, code, ex.getMessage(), req, requestId(req));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> illegalState(IllegalStateException ex, HttpServletRequest req) {
        return handle(HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION", ex.getMessage(), req, requestId(req));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> responseStatus(ResponseStatusException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = status.is5xxServerError()
                ? "The requested service is not currently available"
                : (ex.getReason() != null ? ex.getReason() : status.getReasonPhrase());
        String code = status == HttpStatus.NOT_IMPLEMENTED ? "FEATURE_NOT_IMPLEMENTED" : "REQUEST_FAILED";
        return handle(status, code, message, req, requestId(req));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> general(Exception ex, HttpServletRequest req) {
        return handle(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", req, requestId(req));
    }
}