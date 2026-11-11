package com.sip.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "node_heartbeats")
public class NodeHeartbeat {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id = UUID.randomUUID().toString();

    @Column(name = "node_id", nullable = false)
    private String nodeId;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp = Instant.now();

    @Column(name = "battery_level")
    private Integer batteryLevel;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "storage_available_mb")
    private Long storageAvailableMb;

    @Column(name = "model_version")
    private String modelVersion;

    @Column(name = "queue_depth", nullable = false)
    private int queueDepth = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id", insertable = false, updatable = false)
    private Node node;

    public NodeHeartbeat() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
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
    public int getQueueDepth() { return queueDepth; }
    public void setQueueDepth(int queueDepth) { this.queueDepth = queueDepth; }
    public Node getNode() { return node; }
    public void setNode(Node node) { this.node = node; }
}