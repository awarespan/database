package com.sip.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "evidence")
public class Evidence {
    @Id
    public String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    public Incident incident;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    public EvidenceType type;

    @Column(name = "storage_key")
    public String storageKey;

    @Column(name = "mime_type")
    public String mimeType;

    @Column(name = "size_bytes")
    public Long sizeBytes;

    public String sha256;

    @Column(name = "capture_timestamp")
    public OffsetDateTime captureTimestamp;

    @Column(name = "retention_expiry")
    public OffsetDateTime retentionExpiry;

    @Column(name = "upload_status", nullable = false)
    @Enumerated(EnumType.STRING)
    public UploadStatus uploadStatus = UploadStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    public OffsetDateTime createdAt;

    public enum EvidenceType { IMAGE, VIDEO, AUDIO, THUMBNAIL }
    public enum UploadStatus { PENDING, UPLOADED, FAILED }

    @PrePersist
    public void prePersist() {
        createdAt = OffsetDateTime.now();
    }

    public Evidence() {}
}