package com.sip.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class RefreshRequest {
    @NotBlank
    public String refreshToken;

    public RefreshRequest() {}
    public RefreshRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}