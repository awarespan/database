package com.sip.backend.evidence;

import com.sip.backend.common.ResourceNotFoundException;
import com.sip.backend.entity.Evidence;
import com.sip.backend.repository.EvidenceRepository;
import com.sip.backend.repository.IncidentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EvidenceService {

    private final EvidenceRepository evidenceRepository;
    private final IncidentRepository incidentRepository;

    public EvidenceService(EvidenceRepository evidenceRepository, IncidentRepository incidentRepository) {
        this.evidenceRepository = evidenceRepository;
        this.incidentRepository = incidentRepository;
    }

    @Transactional(readOnly = true)
    public List<Evidence> getEvidenceForIncident(String incidentId) {
        if (!incidentRepository.existsById(incidentId)) {
            throw new ResourceNotFoundException("INCIDENT_NOT_FOUND", "Incident not found: " + incidentId);
        }
        return evidenceRepository.findByIncidentId(incidentId);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> getEvidenceMedia(String incidentId, Evidence.EvidenceType type) {
        if (!incidentRepository.existsById(incidentId)) {
            throw new ResourceNotFoundException("INCIDENT_NOT_FOUND", "Incident not found: " + incidentId);
        }
        List<Evidence> records = evidenceRepository.findByIncidentIdAndType(incidentId, type);
        if (records.isEmpty()) {
            throw new ResourceNotFoundException("Evidence not found for incident " + incidentId + " and type " + type);
        }
        Evidence evidence = records.get(0);
        if (evidence.storageKey == null || evidence.storageKey.isBlank()) {
            throw new ResourceNotFoundException("Evidence file is not available for incident " + incidentId);
        }

        // Never return a successful empty file. An actual object-storage provider must resolve
        // storageKey and stream the bytes before these endpoints can claim to serve media.
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                "Evidence storage provider is not configured");
    }
}
