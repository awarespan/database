package com.sip.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "response_events")
public class ResponseEvent {
    @Id
    public String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    public Incident incident;

    @Column(name = "action_type", nullable = false)
    @Enumerated(EnumType.STRING)
    public ActionType actionType;

    @Column(nullable = false)
    public OffsetDateTime timestamp;

    public String result;

    @Column(nullable = false)
    public Boolean autonomous = false;

    public enum ActionType {
        ALERT, DETERRENCE, ESCALATION, VERIFICATION, REJECTION, RESOLUTION
    }

    @PrePersist
    public void prePersist() {
        if (timestamp == null) timestamp = OffsetDateTime.now();
    }

    public ResponseEvent() {}
}