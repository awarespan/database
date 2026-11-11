package com.sip.backend.incident;

import com.sip.backend.common.ResourceNotFoundException;
import com.sip.backend.dto.*;
import com.sip.backend.entity.*;
import com.sip.backend.repository.*;
import com.sip.backend.audit.AuditService;
import com.sip.backend.realtime.WebSocketBroadcaster;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final NodeRepository nodeRepository;
    private final UserRepository userRepository;
    private final DetectionResultRepository detectionResultRepository;
    private final HumanAnnotationRepository annotationRepository;
    private final EvidenceRepository evidenceRepository;
    private final ResponseEventRepository responseEventRepository;
    private final AuditService auditService;
    private final WebSocketBroadcaster broadcaster;
    private final IncidentMapper mapper;

    public IncidentService(IncidentRepository incidentRepository, NodeRepository nodeRepository,
                           UserRepository userRepository, DetectionResultRepository detectionResultRepository,
                           HumanAnnotationRepository annotationRepository,
                           EvidenceRepository evidenceRepository,
                           ResponseEventRepository responseEventRepository,
                           AuditService auditService, WebSocketBroadcaster broadcaster,
                           IncidentMapper mapper) {
        this.incidentRepository = incidentRepository;
        this.nodeRepository = nodeRepository;
        this.userRepository = userRepository;
        this.detectionResultRepository = detectionResultRepository;
        this.annotationRepository = annotationRepository;
        this.evidenceRepository = evidenceRepository;
        this.responseEventRepository = responseEventRepository;
        this.auditService = auditService;
        this.broadcaster = broadcaster;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageDto<IncidentDto> getIncidents(
            Integer page, Integer size, String state, String threatType,
            String nodeId, String from, String to) {
        int pageIndex = page != null ? page : 0;
        int pageSize = size != null ? size : 50;
        if (pageIndex < 0) throw new IllegalArgumentException("page must be zero or greater");
        if (pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
        PageRequest pr = PageRequest.of(pageIndex, pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Incident.IncidentState stateFilter = parseEnum(state, Incident.IncidentState.class, null);
        Incident.ThreatType typeFilter = parseEnum(threatType, Incident.ThreatType.class, null);
        OffsetDateTime fromDt = parseOffsetDateTime(from);
        OffsetDateTime toDt = parseOffsetDateTime(to);
        if (fromDt != null && toDt != null && fromDt.isAfter(toDt)) {
            throw new IllegalArgumentException("from must be earlier than or equal to to");
        }

        Page<Incident> result;
        if (stateFilter == null && nodeId == null && typeFilter == null && fromDt == null && toDt == null) {
            result = incidentRepository.findAllByOrderByCreatedAtDesc(pr);
        } else {
            result = incidentRepository.search(stateFilter, nodeId, typeFilter, fromDt, toDt, pr);
        }
        List<IncidentDto> dtos = result.getContent().stream().map(mapper::toDto).toList();

        return new PageDto<>(dtos, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public IncidentDto getIncident(String id) {
        Incident incident = findIncidentOrThrow(id);
        return enrichIncident(incident);
    }

    @Transactional
    public IncidentDto verify(String incidentId, VerificationRequest request, String annotatorId) {
        Incident incident = findIncidentOrThrow(incidentId);

        if (incident.state != Incident.IncidentState.PENDING_VERIFICATION) {
            throw new IllegalStateException(
                "Incident must be in PENDING_VERIFICATION state to verify; current: " + incident.state);
        }

        User annotator = userRepository.findById(annotatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        HumanAnnotation.HumanLabel label;
        try {
            label = HumanAnnotation.HumanLabel.valueOf(request.label.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid label: " + request.label +
                ". Must be one of: FALSE_POSITIVE, TRUE_POSITIVE, UNCERTAIN");
        }

        HumanAnnotation annotation = new HumanAnnotation();
        annotation.id = UUID.randomUUID().toString();
        annotation.incident = incident;
        annotation.annotator = annotator;
        annotation.label = label;
        annotation.timestamp = OffsetDateTime.now();
        annotation.notes = request.notes;
        annotation.confidence = request.confidence;
        annotationRepository.save(annotation);

        Incident.IncidentState newState = IncidentStateMachine.nextStateForVerification(label);
        IncidentStateMachine.validateTransition(incident.state, newState);
        incident.state = newState;
        incidentRepository.save(incident);

        ResponseEvent re = new ResponseEvent();
        re.id = UUID.randomUUID().toString();
        re.incident = incident;
        re.actionType = ResponseEvent.ActionType.VERIFICATION;
        re.timestamp = OffsetDateTime.now();
        re.result = label.name();
        re.autonomous = false;
        responseEventRepository.save(re);

        auditService.log("INCIDENT_VERIFIED", incident.id, annotatorId,
            "Label: " + label + ". Notes: " + request.notes);

        // Prepare the response inside the transaction, but notify clients only after commit.
        // Otherwise a client could observe an update that later rolls back.
        IncidentDto response = enrichIncident(incident);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                broadcaster.broadcastIncidentUpdated(response);
            }
        });
        return response;
    }

    @Transactional(readOnly = true)
    public List<AuditEventDto> getIncidentEvents(String incidentId) {
        findIncidentOrThrow(incidentId);
        return auditService.getByIncident(incidentId);
    }

    private IncidentDto enrichIncident(Incident incident) {
        IncidentDto dto = mapper.toDto(incident);
        dto.detection = detectionResultRepository.findByIncidentId(incident.id)
                .map(mapper::toDetectionDto).orElse(null);

        IncidentDetailDto detail = new IncidentDetailDto();
        detail.annotations = annotationRepository.findByIncidentIdOrderByTimestampDesc(incident.id)
                .stream().map(mapper::toAnnotationDto).toList();
        detail.responseEvents = responseEventRepository.findByIncidentIdOrderByTimestampDesc(incident.id)
                .stream().map(mapper::toResponseEventDto).toList();
        detail.evidence = evidenceRepository.findByIncidentId(incident.id)
                .stream().map(mapper::toEvidenceDto).toList();
        dto.detail = detail;

        return dto;
    }

    private Incident findIncidentOrThrow(String id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("INCIDENT_NOT_FOUND",
                    "Incident not found: " + id));
    }

    private <T extends Enum<T>> T parseEnum(String value, Class<T> clazz, T defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Enum.valueOf(clazz, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid " + clazz.getSimpleName() + " filter: " + value);
        }
    }

    private OffsetDateTime parseOffsetDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return OffsetDateTime.parse(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ISO-8601 datetime: " + value);
        }
    }
}