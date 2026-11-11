package com.sip.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "human_annotations")
public class HumanAnnotation {
    @Id
    public String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    public Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annotator_id", nullable = false)
    public User annotator;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    public HumanLabel label;

    @Column(nullable = false)
    public OffsetDateTime timestamp;

    public String notes;

    public Double confidence;

    @Column(nullable = false)
    public Integer version = 1;

    public enum HumanLabel { FALSE_POSITIVE, TRUE_POSITIVE, UNCERTAIN }

    @PrePersist
    public void prePersist() {
        if (timestamp == null) timestamp = OffsetDateTime.now();
        if (version == null) version = 1;
    }

    public HumanAnnotation() {}
}