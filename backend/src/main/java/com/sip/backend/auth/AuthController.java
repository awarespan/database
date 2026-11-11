package com.sip.backend.auth;

import com.sip.backend.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    /**
     * Logout can revoke the supplied refresh token even when the access token has expired.
     * If no refresh token is supplied, a valid access token revokes all sessions for that user.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) RefreshRequest request,
            Authentication auth) {
        if (request != null && request.refreshToken != null && !request.refreshToken.isBlank()) {
            authService.logoutRefreshToken(request.refreshToken);
        } else if (auth != null) {
            authService.logout(auth.getName());
        }
        return ResponseEntity.noContent().build();
    }
}
