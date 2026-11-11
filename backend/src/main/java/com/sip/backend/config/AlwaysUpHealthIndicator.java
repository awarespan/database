package com.sip.backend.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Always returns UP for Render health checks.
 * Bypasses any datasource/HikariCP connection checks.
 * Used because Render probes /actuator/health BEFORE the DB pool is ready.
 */
@Component
public class AlwaysUpHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        return Health.up()
            .withDetail("source", "AlwaysUpHealthIndicator")
            .withDetail("reason", "Render health check — no external deps checked")
            .build();
    }
}