package com.sip.backend.dto;

import java.time.OffsetDateTime;

public class EvidenceDto {
    public String id;
    public String incidentId;
    public String type;
    public String storageKey;
    public String mimeType;
    public Long sizeBytes;
    public String sha256;
    public OffsetDateTime captureTimestamp;
    public String uploadStatus;
    public OffsetDateTime createdAt;

    public EvidenceDto() {}
}