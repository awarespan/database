package com.sip.backend.realtime;

import java.time.OffsetDateTime;
import java.util.UUID;

public class WebSocketEvent {
    public String type;
    public OffsetDateTime timestamp;
    public Object payload;
    public String eventId;

    public WebSocketEvent() {
        this.timestamp = OffsetDateTime.now();
        this.eventId = UUID.randomUUID().toString();
    }

    public static WebSocketEvent of(String type, Object payload) {
        WebSocketEvent event = new WebSocketEvent();
        event.type = type;
        event.payload = payload;
        return event;
    }

    public static final String INCIDENT_NEW = "INCIDENT_NEW";
    public static final String INCIDENT_UPDATED = "INCIDENT_UPDATED";
    public static final String NODE_STATUS_CHANGED = "NODE_STATUS_CHANGED";
    public static final String SYSTEM_ALERT = "SYSTEM_ALERT";
}