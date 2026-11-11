package com.sip.backend.controller;

import com.sip.backend.dto.*;
import com.sip.backend.entity.Incident;
import com.sip.backend.entity.Incident.IncidentState;
import com.sip.backend.incident.IncidentMapper;
import com.sip.backend.incident.IncidentService;
import com.sip.backend.repository.IncidentRepository;
import com.sip.backend.service.PiIncidentIngestionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final IncidentRepository incidentRepository;
    private final IncidentService incidentService;
    private final PiIncidentIngestionService ingestionService;
    private final IncidentMapper mapper;

    public IncidentController(IncidentRepository incidentRepository,
                              IncidentService incidentService,
                              PiIncidentIngestionService ingestionService,
                              IncidentMapper mapper) {
        this.incidentRepository = incidentRepository;
        this.incidentService = incidentService;
        this.ingestionService = ingestionService;
        this.mapper = mapper;
    }

    // ── Pi-facing REST ingestion (when MQTT is unavailable) ──────────────────
    @PostMapping
    public ResponseEntity<?> ingestIncident(@RequestBody PiIncidentPayload payload) {
        try {
            Incident incident = ingestionService.ingestIncident(payload);
            return ResponseEntity.status(201).body(mapper.toDto(incident));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── Android/operator listing & detail ───────────────────────────────────
    @GetMapping
    public ResponseEntity<PageDto<IncidentDto>> getIncidents(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String threatType,
            @RequestParam(required = false) String nodeId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(incidentService.getIncidents(page, size, state, threatType, nodeId, from, to));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentDto> getIncident(@PathVariable String id) {
        return ResponseEntity.ok(incidentService.getIncident(id));
    }

    // ── Human verification ───────────────────────────────────────────────────
    @PostMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<IncidentDto> verify(
            @PathVariable String id,
            @Valid @RequestBody VerificationRequest request,
            Authentication auth) {
        return ResponseEntity.ok(incidentService.verify(id, request, auth.getName()));
    }

    @GetMapping("/{id}/events")
    public ResponseEntity<List<AuditEventDto>> getIncidentEvents(@PathVariable String id) {
        return ResponseEntity.ok(incidentService.getIncidentEvents(id));
    }

    // ── Legacy state transitions (deprecated — use /verify) ──────────────────
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable String id) {
        return incidentRepository.findById(id).map(incident -> {
            incident.state = IncidentState.REJECTED;
            incidentRepository.save(incident);
            return ResponseEntity.ok(incident);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/escalate")
    public ResponseEntity<?> escalate(@PathVariable String id) {
        return incidentRepository.findById(id).map(incident -> {
            incident.state = IncidentState.ESCALATED;
            incidentRepository.save(incident);
            return ResponseEntity.ok(incident);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<?> resolve(@PathVariable String id) {
        return incidentRepository.findById(id).map(incident -> {
            incident.state = IncidentState.RESOLVED;
            incidentRepository.save(incident);
            return ResponseEntity.ok(incident);
        }).orElse(ResponseEntity.notFound().build());
    }
}