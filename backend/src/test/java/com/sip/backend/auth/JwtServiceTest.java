package com.sip.backend.auth;

import com.sip.backend.config.JwtProperties;
import com.sip.backend.entity.User;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("this-is-a-test-secret-key-that-is-at-least-256-bits-long-for-hs256");
        props.setAccessTokenExpirySeconds(900);
        props.setRefreshTokenExpirySeconds(604800);
        jwtService = new JwtService(props);
    }

    @Test
    void generateAccessToken_createsValidToken() {
        User user = new User("user-1", "testuser", "hash", User.Role.OPERATOR);
        String token = jwtService.generateAccessToken(user);
        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
        assertEquals("user-1", jwtService.extractUserId(token));
        assertEquals("OPERATOR", jwtService.extractRole(token));
    }

    @Test
    void generateRefreshToken_createsValidToken() {
        User user = new User("user-1", "testuser", "hash", User.Role.ADMIN);
        String token = jwtService.generateRefreshToken(user);
        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
        assertEquals("user-1", jwtService.extractUserId(token));
    }

    @Test
    void isTokenValid_returnsFalseForTamperedToken() {
        User user = new User("user-1", "testuser", "hash", User.Role.OPERATOR);
        String token = jwtService.generateAccessToken(user);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertFalse(jwtService.isTokenValid(tampered));
    }

    @Test
    void isTokenValid_returnsFalseForNullAndEmpty() {
        assertFalse(jwtService.isTokenValid(null));
        assertFalse(jwtService.isTokenValid(""));
    }
}