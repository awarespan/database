package com.sip.backend.dto;

public class LoginResponse {
    public String token;
    public String refreshToken;
    public long expiresIn;
    public String userId;
    public String username;
    public String role;

    public LoginResponse() {}

    public LoginResponse(String token, String refreshToken, long expiresIn,
                         String userId, String username, String role) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.userId = userId;
        this.username = username;
        this.role = role;
    }
}