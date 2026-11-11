package com.sip.backend.auth;

import com.sip.backend.config.JwtProperties;
import com.sip.backend.entity.RefreshToken;
import com.sip.backend.entity.User;
import com.sip.backend.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository repository;
    @Mock private JwtProperties properties;
    @InjectMocks private RefreshTokenService service;

    @Test
    void createForUserReturnsOpaqueTokenButPersistsOnlyHash() {
        when(properties.getRefreshTokenExpirySeconds()).thenReturn(3600L);
        // Simulate JPA merge behavior where transient fields (rawToken) are not copied to the returned managed instance
        when(repository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> {
                    RefreshToken arg = invocation.getArgument(0);
                    RefreshToken copy = new RefreshToken();
                    copy.id = arg.id;
                    copy.user = arg.user;
                    copy.tokenHash = arg.tokenHash;
                    copy.expiresAt = arg.expiresAt;
                    copy.rawToken = null; // Transient field dropped by merge
                    return copy;
                });
        User user = new User("u-1", "operator", "hash", User.Role.OPERATOR);

        RefreshToken result = service.createForUser(user);

        assertNotNull(result.rawToken, "rawToken must not be null after creation");
        assertFalse(result.rawToken.isBlank());
        assertNotEquals(result.rawToken, result.tokenHash);
        assertEquals(user, result.user);
        assertTrue(result.expiresAt.isAfter(OffsetDateTime.now()));
        verify(repository).save(any(RefreshToken.class));
    }

    @Test
    void validateAndConsumeOnlyReturnsTokenWhenAtomicConsumeSucceeds() {
        RefreshToken token = new RefreshToken();
        token.id = "rt-1";
        token.tokenHash = "stored-hash";
        token.user = new User("u-1", "operator", "hash", User.Role.OPERATOR);
        token.expiresAt = OffsetDateTime.now().plusHours(1);
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(repository.consumeIfValid(anyString(), any(OffsetDateTime.class))).thenReturn(1);

        RefreshToken consumed = service.validateAndConsume("presented-token");

        assertSame(token, consumed);
        assertNotNull(consumed.revokedAt);
        verify(repository).consumeIfValid(anyString(), any(OffsetDateTime.class));
    }

    @Test
    void validateAndConsumeRejectsConcurrentReplay() {
        RefreshToken token = new RefreshToken();
        token.id = "rt-1";
        token.tokenHash = "stored-hash";
        token.user = new User("u-1", "operator", "hash", User.Role.OPERATOR);
        token.expiresAt = OffsetDateTime.now().plusHours(1);
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(repository.consumeIfValid(anyString(), any(OffsetDateTime.class))).thenReturn(0);

        assertNull(service.validateAndConsume("presented-token"));
        assertNull(token.revokedAt);
    }
}
