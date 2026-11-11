package com.sip.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "model_versions")
public class ModelVersion {
    @Id
    public String version;

    @Column(name = "model_name", nullable = false)
    public String modelName;

    @Column(name = "trained_at")
    public OffsetDateTime trainedAt;

    @Column(name = "dataset_hash")
    public String datasetHash;

    @Column(name = "validation_map50")
    public Double validationMap50;

    public ModelVersion() {}
}