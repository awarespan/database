package com.sip.backend.dto;

import java.time.OffsetDateTime;

public class AnnotationDto {
    public String id;
    public String incidentId;
    public String annotatorId;
    public String annotatorUsername;
    public String label;
    public OffsetDateTime timestamp;
    public String notes;
    public Double confidence;
    public Integer version;

    public AnnotationDto() {}
}