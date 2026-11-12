package com.sip.backend.config;

import com.sip.backend.service.PiAuthenticationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Authenticates Pi edge nodes via X-API-Key or per-node X-Device-Secret/X-Node-Token.
 * Sets ROLE_DEVICE / ROLE_NODE in SecurityContextHolder so Spring Security authorizes the request.
 */
@Component
@Order(1)
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-Key";
    public static final String NODE_ID_HEADER = "X-Node-Id";
    public static final String DEVICE_SECRET_HEADER = "X-Device-Secret";
    public static final String NODE_TOKEN_HEADER = "X-Node-Token";

    private final String validApiKey;
    private final PiAuthenticationService piAuthService;

    public ApiKeyAuthFilter(
            @Value("${app.device-api-key:}") String validApiKey,
            PiAuthenticationService piAuthService) {
        this.validApiKey = (validApiKey != null && !validApiKey.isBlank()) ? validApiKey.trim() : null;
        this.piAuthService = piAuthService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Check if this is a device-facing endpoint
        boolean isDeviceEndpoint = 
            (path.startsWith("/api/v1/nodes/") && path.endsWith("/heartbeat")) ||
            ("POST".equalsIgnoreCase(method) && path.startsWith("/api/v1/incidents")) ||
            path.startsWith("/api/v1/commands/") ||
            path.startsWith("/api/v1/media/");

        if (isDeviceEndpoint) {
            // If already authenticated (e.g. by JWT earlier), proceed
            if (SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
                filterChain.doFilter(request, response);
                return;
            }

            String presentedApiKey = request.getHeader(API_KEY_HEADER);
            String presentedNodeId = request.getHeader(NODE_ID_HEADER);
            String presentedDeviceSecret = request.getHeader(DEVICE_SECRET_HEADER);
            String presentedNodeToken = request.getHeader(NODE_TOKEN_HEADER);

            // 1. Check per-node authentication if node headers are provided
            if (presentedNodeId != null && !presentedNodeId.isBlank()) {
                boolean authenticated = false;
                if (presentedDeviceSecret != null && !presentedDeviceSecret.isBlank()) {
                    authenticated = piAuthService.validateDeviceSecret(presentedNodeId, presentedDeviceSecret);
                } else if (presentedNodeToken != null && !presentedNodeToken.isBlank()) {
                    authenticated = piAuthService.validateNodeToken(presentedNodeId, presentedNodeToken);
                }

                if (authenticated) {
                    var auth = new UsernamePasswordAuthenticationToken(
                        presentedNodeId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_DEVICE"), new SimpleGrantedAuthority("ROLE_NODE"))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            // 2. Check global device API key
            if (validApiKey != null && presentedApiKey != null && !presentedApiKey.isBlank()) {
                if (MessageDigest.isEqual(validApiKey.getBytes(StandardCharsets.UTF_8),
                                          presentedApiKey.getBytes(StandardCharsets.UTF_8))) {
                    var auth = new UsernamePasswordAuthenticationToken(
                        "DEVICE",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_DEVICE"), new SimpleGrantedAuthority("ROLE_NODE"))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            // If neither authentication method matched, check if an API key was presented and invalid
            if (presentedApiKey != null && !presentedApiKey.isBlank()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Invalid API key\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
