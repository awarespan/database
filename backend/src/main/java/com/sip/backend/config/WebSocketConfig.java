package com.sip.backend.config;

import com.sip.backend.auth.JwtHandshakeInterceptor;
import com.sip.backend.realtime.WebSocketBroadcaster;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketBroadcaster broadcaster;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    public WebSocketConfig(WebSocketBroadcaster broadcaster,
                           JwtHandshakeInterceptor jwtHandshakeInterceptor) {
        this.broadcaster = broadcaster;
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(broadcaster, "/ws/events")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOrigins(parseOrigins());
    }

    private String[] parseOrigins() {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return new String[]{"*"};
        }
        return allowedOrigins.split(",");
    }
}
