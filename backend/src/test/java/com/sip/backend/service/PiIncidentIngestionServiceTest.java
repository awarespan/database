package com.sip.backend.service;

import com.sip.backend.dto.PiIncidentPayload;
import com.sip.backend.dto.IncidentDto;
import com.sip.backend.dto.DetectionResultDto;
import com.sip.backend.dto.ResponseEventDto;
import com.sip.backend.entity.DetectionResult;
import com.sip.backend.entity.Incident;
import com.sip.backend.entity.Node;
import com.sip.backend.entity.ResponseEvent;
import com.sip.backend.incident.IncidentMapper;
import com.sip.backend.realtime.WebSocketBroadcaster;
import com.sip.backend.repository.DetectionResultRepository;
import com.sip.backend.repository.IncidentRepository;
import com.sip.backend.repository.NodeRepository;
import com.sip.backend.repository.ResponseEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PiIncidentIngestionServiceTest {
    @Mock IncidentRepository incidents;
    @Mock DetectionResultRepository detections;
    @Mock NodeRepository nodes;
    @Mock ResponseEventRepository responses;
    @Mock WebSocketBroadcaster broadcaster;
    @Mock IncidentMapper mapper;

    @Test
    void persistsIndividualPiDetectionsAndDeterrenceAndIsIdempotent() {
        PiIncidentIngestionService service = new PiIncidentIngestionService(
                incidents, detections, nodes, responses, broadcaster, mapper);
        Node node = new Node();
        node.id = "node-ci-01";
        node.name = "CI node";
        node.status = Node.NodeStatus.ONLINE;
        when(nodes.findById("node-ci-01")).thenReturn(Optional.of(node));
        when(incidents.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(detections.save(any(DetectionResult.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DetectionResult persistedDetection = new DetectionResult();
        persistedDetection.predictedClass = "gun,knife";
        persistedDetection.confidence = 0.93;
        when(detections.findByIncidentId("evt-ci-0001")).thenReturn(Optional.of(persistedDetection));
        when(responses.save(any(ResponseEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDto(any(Incident.class))).thenReturn(new IncidentDto());
        DetectionResultDto detectionDto = new DetectionResultDto();
        detectionDto.predictedClass = "gun,knife";
        detectionDto.confidence = 0.93;
        when(mapper.toDetectionDto(any(DetectionResult.class))).thenReturn(detectionDto);
        when(mapper.toResponseEventDto(any(ResponseEvent.class))).thenReturn(new ResponseEventDto());

        PiIncidentPayload payload = new PiIncidentPayload();
        payload.setIncidentId("evt-ci-0001");
        payload.setIdempotencyKey("evt-ci-0001");
        payload.setNodeId("node-ci-01");
        payload.setDetectionTime(Instant.parse("2026-11-17T12:00:00Z"));
        payload.setThreatType("WEAPON");
        payload.setThreatSeverity("HIGH");
        payload.setThreatDescription("Detected gun, knife");
        payload.setConfidence(0.93);
        payload.setModelVersion("awarespan-weapon-yolov8n:1.0.0");
        payload.setTier(1);
        payload.setDetectionDomain("WEAPON");
        payload.setDetections(List.of(
                Map.of("label", "gun", "confidence", 0.93),
                Map.of("label", "knife", "confidence", 0.81)));
        payload.setDeterrence(Map.of("attempted", true, "started", false, "reason", "speaker_disabled"));

        Incident saved = service.ingestIncident(payload);
        assertEquals("evt-ci-0001", saved.id);
        ArgumentCaptor<DetectionResult> detectionCaptor = ArgumentCaptor.forClass(DetectionResult.class);
        verify(detections).save(detectionCaptor.capture());
        assertEquals("gun,knife", detectionCaptor.getValue().predictedClass);
        assertEquals(0.93, detectionCaptor.getValue().confidence);
        assertTrue(detectionCaptor.getValue().rawScores.contains("\"knife\""));
        assertEquals("[\"visual\"]", detectionCaptor.getValue().sensorModalities);

        ArgumentCaptor<ResponseEvent> responseCaptor = ArgumentCaptor.forClass(ResponseEvent.class);
        verify(responses).save(responseCaptor.capture());
        assertEquals(ResponseEvent.ActionType.DETERRENCE, responseCaptor.getValue().actionType);
        assertTrue(responseCaptor.getValue().result.contains("NOT_STARTED"));
        ArgumentCaptor<IncidentDto> broadcastCaptor = ArgumentCaptor.forClass(IncidentDto.class);
        verify(broadcaster).broadcastIncidentNew(broadcastCaptor.capture());
        assertEquals("gun,knife", broadcastCaptor.getValue().detection.predictedClass);
        assertEquals(1, broadcastCaptor.getValue().responseEvents.size());

        when(incidents.findByIdempotencyKey("evt-ci-0001")).thenReturn(Optional.of(saved));
        assertSame(saved, service.ingestIncident(payload));
        verify(incidents, times(1)).save(any(Incident.class));
        verify(detections, times(1)).save(any(DetectionResult.class));
        verify(responses, times(1)).save(any(ResponseEvent.class));
    }
}
