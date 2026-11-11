package com.sip.backend.dto;

import java.time.OffsetDateTime;

public class AuditEventDto {
    public String id;
    public String incidentId;
    public String eventType;
    public OffsetDateTime timestamp;
    public String description;
    public String actor;

    public AuditEventDto() {}
}