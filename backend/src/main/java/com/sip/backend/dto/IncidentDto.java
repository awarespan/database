package com.sip.backend.dto;

import java.time.OffsetDateTime;

public class IncidentDto {
    public String id;
    public String state;
    public String threatType;
    public String threatSeverity;
    public String threatDescription;
    public Double latitude;
    public Double longitude;
    public String locationReadable;
    public Double locationAccuracy;
    public String nodeId;
    public String nodeName;
    public Boolean autopilotHandled;
    public OffsetDateTime createdAt;
    public OffsetDateTime updatedAt;
    public DetectionResultDto detection;
    public IncidentDetailDto detail;

    public IncidentDto() {}
}