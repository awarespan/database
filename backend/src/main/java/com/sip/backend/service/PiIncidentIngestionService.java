package com.sip.backend.service;

import com.sip.backend.dto.PiIncidentPayload;
import com.sip.backend.entity.DetectionResult;
import com.sip.backend.entity.Incident;
import com.sip.backend.entity.Incident.*;
import com.sip.backend.entity.Node;
import com.sip.backend.repository.DetectionResultRepository;
import com.sip.backend.repository.IncidentRepository;
import com.sip.backend.repository.NodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PiIncidentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(PiIncidentIngestionService.class);

    private final IncidentRepository incidentRepository;
    private final DetectionResultRepository detectionResultRepository;
    private final NodeRepository nodeRepository;
    private final com.sip.backend.realtime.WebSocketBroadcaster broadcaster;
    private final com.sip.backend.incident.IncidentMapper mapper;

    public PiIncidentIngestionService(
            IncidentRepository incidentRepository,
            DetectionResultRepository detectionResultRepository,
            NodeRepository nodeRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) com.sip.backend.realtime.WebSocketBroadcaster broadcaster,
            @org.springframework.beans.factory.annotation.Autowired(required = false) com.sip.backend.incident.IncidentMapper mapper) {
        this.incidentRepository = incidentRepository;
        this.detectionResultRepository = detectionResultRepository;
        this.nodeRepository = nodeRepository;
        this.broadcaster = broadcaster;
        this.mapper = mapper;
    }

    /**
     * Ingests an incident from a Pi via MQTT, idempotently.
     * If the idempotency_key already exists, returns the existing incident.
     */
    @Transactional
    public Incident ingestIncident(PiIncidentPayload payload) {
        // Idempotency check
        if (payload.getIdempotencyKey() != null && !payload.getIdempotencyKey().isBlank()) {
            Optional<Incident> existing = incidentRepository.findByIdempotencyKey(payload.getIdempotencyKey());
            if (existing.isPresent()) {
                log.debug("Duplicate incident skipped via idempotency key: {}", payload.getIdempotencyKey());
                return existing.get();
            }
        }

        // Validate or auto-register node
        Node node = nodeRepository.findById(payload.getNodeId())
                .orElseGet(() -> {
                    Node newNode = new Node();
                    newNode.id = payload.getNodeId();
                    newNode.name = "Raspberry Pi (" + payload.getNodeId() + ")";
                    newNode.status = Node.NodeStatus.ONLINE;
                    newNode.deviceSecret = payload.getNodeId();
                    log.info("Auto-registered new node on incident ingestion: {}", payload.getNodeId());
                    return nodeRepository.save(newNode);
                });

        int tier = payload.getTier() != null ? payload.getTier() : 0;

        // Map string threat type to enum
        ThreatType threatType = parseThreatType(payload.getThreatType());
        ThreatSeverity severity = parseSeverity(payload.getThreatSeverity());
        DetectionDomain domain = parseDomain(payload.getDetectionDomain());

        Incident incident = new Incident();
        incident.id = payload.getIncidentId() != null ? payload.getIncidentId() : UUID.randomUUID().toString();
        incident.state = IncidentState.DETECTED;
        incident.threatType = threatType;
        incident.threatSeverity = severity;
        incident.threatDescription = payload.getThreatDescription();
        incident.latitude = payload.getLatitude();
        incident.longitude = payload.getLongitude();
        incident.locationReadable = payload.getLocationReadable();
        incident.locationAccuracy = payload.getLocationAccuracy();
        incident.node = node;
        incident.autopilotHandled = false;
        incident.createdAt = payload.getDetectionTime() != null
                ? OffsetDateTime.ofInstant(payload.getDetectionTime(), java.time.ZoneOffset.UTC)
                : OffsetDateTime.now();
        incident.updatedAt = OffsetDateTime.now();
        incident.syncStatus = payload.getSyncStatus() != null ? payload.getSyncStatus() : "SYNCED";
        incident.idempotencyKey = payload.getIdempotencyKey();
        incident.tier = tier;
        incident.detectionDomain = domain;

        incident = incidentRepository.save(incident);

        // Store detection result if model version and confidence provided
        if (payload.getModelVersion() != null && payload.getConfidence() != null) {
            DetectionResult dr = new DetectionResult();
            dr.id = UUID.randomUUID().toString();
            dr.incident = incident;
            dr.predictedClass = payload.getThreatType();
            dr.confidence = payload.getConfidence();
            dr.modelVersion = payload.getModelVersion();
            dr.detectionTimestamp = incident.createdAt;
            dr.sensorModalities = null;
            dr.rawScores = null;
            detectionResultRepository.save(dr);
        }

        // Update node last heartbeat
        node.lastHeartbeat = OffsetDateTime.now();
        node.status = Node.NodeStatus.ONLINE;
        nodeRepository.save(node);

        log.info("Ingested incident {} from node {} tier={} domain={}",
                incident.id, payload.getNodeId(), tier, payload.getDetectionDomain());

        if (broadcaster != null && mapper != null) {
            try {
                broadcaster.broadcastIncidentNew(mapper.toDto(incident));
                log.info("Broadcasted real-time incident {} to websocket clients", incident.id);
            } catch (Exception e) {
                log.warn("Failed to broadcast real-time incident {}: {}", incident.id, e.getMessage());
            }
        }

        return incident;
    }

    private ThreatType parseThreatType(String s) {
        if (s == null || s.isBlank()) return ThreatType.UNKNOWN;
        try { return ThreatType.valueOf(s.toUpperCase().replace(" ", "_").replace(",", "_")); }
        catch (IllegalArgumentException e) { return ThreatType.UNKNOWN; }
    }

    private ThreatSeverity parseSeverity(String s) {
        if (s == null || s.isBlank()) return ThreatSeverity.LOW;
        try { return ThreatSeverity.valueOf(s.toUpperCase().replace("-", "_").replace(" ", "_")); }
        catch (IllegalArgumentException e) { return ThreatSeverity.LOW; }
    }

    private DetectionDomain parseDomain(String s) {
        if (s == null || s.isBlank()) return null;
        try { return DetectionDomain.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }
}