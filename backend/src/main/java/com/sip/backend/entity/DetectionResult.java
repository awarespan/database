package com.sip.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "detection_results")
public class DetectionResult {
    @Id
    public String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false, unique = true)
    public Incident incident;

    @Column(name = "predicted_class", nullable = false)
    public String predictedClass;

    @Column(nullable = false)
    public Double confidence;

    @Column(name = "model_version", nullable = false)
    public String modelVersion;

    @Column(name = "detection_timestamp", nullable = false)
    public OffsetDateTime detectionTimestamp;

    @Column(name = "sensor_modalities", columnDefinition = "jsonb")
    public String sensorModalities;

    @Column(name = "raw_scores", columnDefinition = "jsonb")
    public String rawScores;

    public DetectionResult() {}
}