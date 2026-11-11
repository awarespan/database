package com.sip.backend.auth;

import com.sip.backend.config.JwtProperties;
import com.sip.backend.entity.RefreshToken;
import com.sip.backend.entity.User;
import com.sip.backend.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public RefreshToken createForUser(User user) {
        // Use a high-entropy opaque token. Persist only its SHA-256 hash.
        String tokenValue = UUID.randomUUID() + "." + UUID.randomUUID();
        RefreshToken rt = new RefreshToken();
        rt.id = UUID.randomUUID().toString();
        rt.user = user;
        rt.tokenHash = hash(tokenValue);
        rt.expiresAt = OffsetDateTime.now().plusSeconds(jwtProperties.getRefreshTokenExpirySeconds());
        rt.rawToken = tokenValue;
        RefreshToken saved = refreshTokenRepository.save(rt);
        saved.rawToken = tokenValue;
        return saved;
    }

    @Transactional
    public RefreshToken validateAndConsume(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) return null;
        String tokenHash = hash(tokenValue);
        RefreshToken rt = refreshTokenRepository.findByTokenHash(tokenHash).orElse(null);
        if (rt == null || !rt.isValid()) return null;

        // Conditional update makes rotation one-time even when two requests race.
        OffsetDateTime now = OffsetDateTime.now();
        if (refreshTokenRepository.consumeIfValid(tokenHash, now) != 1) return null;
        rt.revokedAt = now;
        return rt;
    }

    @Transactional
    public void revokeAllForUser(String userId) {
        refreshTokenRepository.revokeAllForUser(userId, OffsetDateTime.now());
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
