package com.sip.backend.audit;

import com.sip.backend.dto.AuditEventDto;
import com.sip.backend.entity.AuditEvent;
import com.sip.backend.entity.Incident;
import com.sip.backend.repository.AuditEventRepository;
import com.sip.backend.repository.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final IncidentRepository incidentRepository;

    public AuditService(AuditEventRepository auditEventRepository,
                        IncidentRepository incidentRepository) {
        this.auditEventRepository = auditEventRepository;
        this.incidentRepository = incidentRepository;
    }

    @Transactional
    public void log(String eventType, String incidentId, String actor, String description) {
        AuditEvent event = new AuditEvent();
        event.id = UUID.randomUUID().toString();
        event.eventType = eventType;
        event.timestamp = OffsetDateTime.now();
        event.actor = actor;
        event.description = description;
        if (incidentId != null) {
            incidentRepository.findById(incidentId).ifPresent(incident -> event.incident = incident);
        }
        auditEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<AuditEventDto> getByIncident(String incidentId) {
        return auditEventRepository.findByIncidentIdOrderByTimestampDesc(incidentId)
                .stream().map(this::toDto).toList();
    }

    private AuditEventDto toDto(AuditEvent e) {
        AuditEventDto dto = new AuditEventDto();
        dto.id = e.id;
        dto.eventType = e.eventType;
        dto.timestamp = e.timestamp;
        dto.description = e.description;
        dto.actor = e.actor;
        dto.incidentId = e.incident != null ? e.incident.id : null;
        return dto;
    }
}