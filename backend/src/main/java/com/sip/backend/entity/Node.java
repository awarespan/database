package com.sip.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "nodes")
public class Node {
    @Id
    public String id;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    public NodeStatus status = NodeStatus.UNKNOWN;

    public Double latitude;
    public Double longitude;

    @Column(name = "last_heartbeat")
    public OffsetDateTime lastHeartbeat;

    @Column(name = "battery_level")
    public Integer batteryLevel;

    @Column(name = "firmware_version")
    public String firmwareVersion;

    @Column(name = "device_secret")
    public String deviceSecret;

    @Column(name = "created_at", nullable = false, updatable = false)
    public OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    public OffsetDateTime updatedAt;

    public enum NodeStatus { ONLINE, DEGRADED, OFFLINE, UNKNOWN }

    @PrePersist
    public void prePersist() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Node() {}
}