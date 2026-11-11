package com.sip.backend.auth;

import com.sip.backend.dto.*;
import com.sip.backend.entity.User;
import com.sip.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authManager;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks private AuthService authService;

    @Test
    void login_withValidCredentials_returnsTokens() {
        User user = new User("id-1", "admin", "hash", User.Role.ADMIN);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.getAccessTokenExpirySeconds()).thenReturn(900L);
        var rt = new com.sip.backend.entity.RefreshToken();
        rt.id = "rt-1";
        rt.rawToken = "opaque-refresh-token";
        rt.user = user;
        when(refreshTokenService.createForUser(user)).thenReturn(rt);

        LoginResponse resp = authService.login(new LoginRequest("admin", "admin123"));

        assertNotNull(resp);
        assertEquals("access-token", resp.token);
        assertEquals("opaque-refresh-token", resp.refreshToken);
        assertEquals("id-1", resp.userId);
        assertEquals("admin", resp.username);
        assertEquals("ADMIN", resp.role);
    }

    @Test
    void login_withInvalidPassword_throwsBadCredentials() {
        doThrow(BadCredentialsException.class).when(authManager).authenticate(any());
        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequest("admin", "wrong")));
    }
}
