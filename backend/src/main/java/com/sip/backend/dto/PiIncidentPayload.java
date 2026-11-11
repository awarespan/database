package com.sip.backend.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class PiIncidentPayload {
    private String incidentId;
    private String idempotencyKey;
    private String nodeId;
    private Instant detectionTime;
    private String threatType;
    private String threatSeverity;
    private String threatDescription;
    private Double latitude;
    private Double longitude;
    private String locationReadable;
    private Double locationAccuracy;
    private String modelVersion;
    private Double confidence;
    private List<Map<String, Object>> detections;
    private Integer tier;
    private String detectionDomain;
    private String syncStatus;

    public PiIncidentPayload() {}

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public Instant getDetectionTime() { return detectionTime; }
    public void setDetectionTime(Instant detectionTime) { this.detectionTime = detectionTime; }
    public String getThreatType() { return threatType; }
    public void setThreatType(String threatType) { this.threatType = threatType; }
    public String getThreatSeverity() { return threatSeverity; }
    public void setThreatSeverity(String threatSeverity) { this.threatSeverity = threatSeverity; }
    public String getThreatDescription() { return threatDescription; }
    public void setThreatDescription(String threatDescription) { this.threatDescription = threatDescription; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getLocationReadable() { return locationReadable; }
    public void setLocationReadable(String locationReadable) { this.locationReadable = locationReadable; }
    public Double getLocationAccuracy() { return locationAccuracy; }
    public void setLocationAccuracy(Double locationAccuracy) { this.locationAccuracy = locationAccuracy; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public List<Map<String, Object>> getDetections() { return detections; }
    public void setDetections(List<Map<String, Object>> detections) { this.detections = detections; }
    public Integer getTier() { return tier; }
    public void setTier(Integer tier) { this.tier = tier; }
    public String getDetectionDomain() { return detectionDomain; }
    public void setDetectionDomain(String detectionDomain) { this.detectionDomain = detectionDomain; }
    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
}