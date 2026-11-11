package com.sip.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;

public class DetectionResultDto {
    public String id;
    public String predictedClass;
    public Double confidence;
    public String modelVersion;
    public OffsetDateTime detectionTimestamp;
    public List<String> sensorModalities;

    public DetectionResultDto() {}
}