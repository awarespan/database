package com.sip.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    public String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    public Incident incident;

    @Column(name = "event_type", nullable = false)
    public String eventType;

    @Column(nullable = false)
    public OffsetDateTime timestamp;

    public String description;

    public String actor;

    @PrePersist
    public void prePersist() {
        if (timestamp == null) timestamp = OffsetDateTime.now();
    }

    public AuditEvent() {}
}