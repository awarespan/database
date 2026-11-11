package com.sip.backend.common;

import java.time.OffsetDateTime;

public class ApiError {
    public OffsetDateTime timestamp;
    public int status;
    public String code;
    public String message;
    public String path;
    public String requestId;

    public ApiError() {
        this.timestamp = OffsetDateTime.now();
    }

    public ApiError(int status, String code, String message, String path, String requestId) {
        this();
        this.status = status;
        this.code = code;
        this.message = message;
        this.path = path;
        this.requestId = requestId;
    }

    public static ApiError of(int status, String code, String message, String path, String requestId) {
        return new ApiError(status, code, message, path, requestId);
    }
}