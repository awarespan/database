package com.sip.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret;
    private long accessTokenExpirySeconds = 900;
    private long refreshTokenExpirySeconds = 604800;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public long getAccessTokenExpirySeconds() { return accessTokenExpirySeconds; }
    public void setAccessTokenExpirySeconds(long v) { this.accessTokenExpirySeconds = v; }
    public long getRefreshTokenExpirySeconds() { return refreshTokenExpirySeconds; }
    public void setRefreshTokenExpirySeconds(long v) { this.refreshTokenExpirySeconds = v; }
}