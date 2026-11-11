package com.sip.backend.auth;

import com.sip.backend.dto.LoginRequest;
import com.sip.backend.dto.LoginResponse;
import com.sip.backend.dto.RefreshRequest;
import com.sip.backend.entity.User;
import com.sip.backend.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(AuthenticationManager authManager, UserRepository userRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService,
                       RefreshTokenService refreshTokenService) {
        this.authManager = authManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    public LoginResponse login(LoginRequest request) {
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username, request.password));
        } catch (Exception e) {
            // Do not disclose whether the username exists or the account is disabled.
            throw new BadCredentialsException("Invalid username or password");
        }

        User user = userRepository.findByUsername(request.username)
                .filter(u -> Boolean.TRUE.equals(u.enabled))
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        var refreshToken = refreshTokenService.createForUser(user);
        return response(user, refreshToken);
    }

    @Transactional
    public LoginResponse refresh(RefreshRequest request) {
        var consumed = refreshTokenService.validateAndConsume(request.refreshToken);
        if (consumed == null) {
            throw new BadCredentialsException("Refresh token is invalid or expired");
        }
        User user = consumed.user;
        if (user == null || !Boolean.TRUE.equals(user.enabled)) {
            throw new BadCredentialsException("Refresh token is invalid or expired");
        }
        return response(user, refreshTokenService.createForUser(user));
    }

    private LoginResponse response(User user, com.sip.backend.entity.RefreshToken refreshToken) {
        return new LoginResponse(
                jwtService.generateAccessToken(user),
                refreshToken.rawToken,
                jwtService.getAccessTokenExpirySeconds(),
                user.id,
                user.username,
                user.role.name()
        );
    }

    public void logoutRefreshToken(String refreshToken) {
        // Opaque refresh tokens are high entropy; invalid tokens intentionally remain a no-op.
        refreshTokenService.validateAndConsume(refreshToken);
    }

    @Transactional
    public void logout(String userId) {
        refreshTokenService.revokeAllForUser(userId);
    }
}
