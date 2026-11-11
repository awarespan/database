package com.sip.backend.dto;

import java.time.OffsetDateTime;

public class NodeDto {
    public String id;
    public String name;
    public String status;
    public Double latitude;
    public Double longitude;
    public OffsetDateTime lastHeartbeat;
    public Integer batteryLevel;
    public String firmwareVersion;
    public OffsetDateTime createdAt;
    public OffsetDateTime updatedAt;

    public NodeDto() {}
}