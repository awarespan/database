package com.sip.backend.dto;

import java.time.Instant;
import java.util.Map;

public class NodeHeartbeatPayload {
    private String nodeId;
    private Instant timestamp;
    private Integer batteryLevel;
    private Double temperature;
    private Long storageAvailableMb;
    private String modelVersion;
    private Integer queueDepth;

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public Integer getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(Integer batteryLevel) { this.batteryLevel = batteryLevel; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Long getStorageAvailableMb() { return storageAvailableMb; }
    public void setStorageAvailableMb(Long storageAvailableMb) { this.storageAvailableMb = storageAvailableMb; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public Integer getQueueDepth() { return queueDepth; }
    public void setQueueDepth(Integer queueDepth) { this.queueDepth = queueDepth; }
}