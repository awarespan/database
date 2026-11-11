package com.sip.backend.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sip.backend.dto.IncidentDto;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class WebSocketBroadcaster extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final Map<String, WebSocketSession> authedSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        objectMapper.findAndRegisterModules();
    }

    @PreDestroy
    public void destroy() {
        sessions.forEach(s -> {
            try { s.close(); } catch (Exception ignored) {}
        });
    }

    public void broadcast(WebSocketEvent event) {
        String json;
        try {
            json = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            return;
        }
        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try { session.sendMessage(message); } catch (Exception ignored) {}
            }
        }
    }

    public void broadcastIncidentUpdated(IncidentDto incident) {
        broadcast(WebSocketEvent.of(WebSocketEvent.INCIDENT_UPDATED, incident));
    }

    public void broadcastIncidentNew(IncidentDto incident) {
        broadcast(WebSocketEvent.of(WebSocketEvent.INCIDENT_NEW, incident));
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        authedSessions.values().removeIf(s -> s.equals(session));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    }
}