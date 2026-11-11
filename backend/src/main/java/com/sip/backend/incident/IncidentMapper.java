package com.sip.backend.incident;

import com.sip.backend.dto.*;
import com.sip.backend.entity.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IncidentMapper {
    private static final ObjectMapper OM = new ObjectMapper();

    public IncidentDto toDto(Incident incident) {
        if (incident == null) return null;
        IncidentDto dto = new IncidentDto();
        dto.id = incident.id;
        dto.state = incident.state.name();
        dto.threatType = incident.threatType.name();
        dto.threatSeverity = incident.threatSeverity.name();
        dto.threatDescription = incident.threatDescription;
        dto.latitude = incident.latitude;
        dto.longitude = incident.longitude;
        dto.locationReadable = incident.locationReadable;
        dto.locationAccuracy = incident.locationAccuracy;
        dto.autopilotHandled = incident.autopilotHandled;
        dto.createdAt = incident.createdAt;
        dto.updatedAt = incident.updatedAt;
        if (incident.node != null) {
            dto.nodeId = incident.node.id;
            dto.nodeName = incident.node.name;
        }
        return dto;
    }

    public DetectionResultDto toDetectionDto(DetectionResult dr) {
        if (dr == null) return null;
        DetectionResultDto dto = new DetectionResultDto();
        dto.id = dr.id;
        dto.predictedClass = dr.predictedClass;
        dto.confidence = dr.confidence;
        dto.modelVersion = dr.modelVersion;
        dto.detectionTimestamp = dr.detectionTimestamp;
        dto.sensorModalities = parseJsonArray(dr.sensorModalities);
        return dto;
    }

    public AnnotationDto toAnnotationDto(HumanAnnotation ha) {
        if (ha == null) return null;
        AnnotationDto dto = new AnnotationDto();
        dto.id = ha.id;
        dto.incidentId = ha.incident != null ? ha.incident.id : null;
        dto.annotatorId = ha.annotator != null ? ha.annotator.id : null;
        dto.annotatorUsername = ha.annotator != null ? ha.annotator.username : null;
        dto.label = ha.label.name();
        dto.timestamp = ha.timestamp;
        dto.notes = ha.notes;
        dto.confidence = ha.confidence;
        dto.version = ha.version;
        return dto;
    }

    public ResponseEventDto toResponseEventDto(ResponseEvent re) {
        if (re == null) return null;
        ResponseEventDto dto = new ResponseEventDto();
        dto.id = re.id;
        dto.incidentId = re.incident != null ? re.incident.id : null;
        dto.actionType = re.actionType.name();
        dto.timestamp = re.timestamp;
        dto.result = re.result;
        dto.autonomous = re.autonomous;
        return dto;
    }

    public EvidenceDto toEvidenceDto(Evidence e) {
        if (e == null) return null;
        EvidenceDto dto = new EvidenceDto();
        dto.id = e.id;
        dto.incidentId = e.incident != null ? e.incident.id : null;
        dto.type = e.type.name();
        dto.storageKey = e.storageKey;
        dto.mimeType = e.mimeType;
        dto.sizeBytes = e.sizeBytes;
        dto.sha256 = e.sha256;
        dto.captureTimestamp = e.captureTimestamp;
        dto.uploadStatus = e.uploadStatus.name();
        dto.createdAt = e.createdAt;
        return dto;
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return OM.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}