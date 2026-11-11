package com.sip.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Simple health check controller for Render and debugging.
 * Serves /, /health, and /actuator/health without requiring auth.
 */
@RestController
public class HealthController {

    /** Simple root health for debugging */
    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of("status", "ok", "service", "sip-backend");
    }

    /** Additional health alias */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "sip-backend");
    }

    /** Actuator health alias */
    @GetMapping("/actuator/health")
    public Map<String, String> actuatorHealth() {
        return Map.of("status", "UP", "service", "sip-backend");
    }
}
