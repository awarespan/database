package com.sip.backend.service;

import com.sip.backend.dto.IncidentDto;
import com.sip.backend.dto.PiIncidentPayload;
import com.sip.backend.entity.Node;
import com.sip.backend.incident.IncidentService;
import com.sip.backend.evidence.EvidenceService;
import com.sip.backend.entity.Evidence;
import org.springframework.core.io.ByteArrayResource;
import com.sip.backend.repository.DetectionResultRepository;
import com.sip.backend.repository.IncidentRepository;
import com.sip.backend.repository.NodeRepository;
import com.sip.backend.repository.ResponseEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PiIncidentPipelineIntegrationTest {
    @Autowired PiIncidentIngestionService ingestionService;
    @Autowired IncidentService incidentService;
    @Autowired NodeRepository nodeRepository;
    @Autowired IncidentRepository incidentRepository;
    @Autowired DetectionResultRepository detectionRepository;
    @Autowired ResponseEventRepository responseEventRepository;
    @Autowired EvidenceService evidenceService;

    @Test
    void ingestsPiDetectionPersistsLabelsAndResponseAndReturnsAndroidDto() {
        Node node = new Node();
        node.id = "pipeline-integration-node";
        node.name = "Pipeline integration node";
        node.status = Node.NodeStatus.ONLINE;
        node.deviceSecret = "integration-secret";
        nodeRepository.save(node);

        PiIncidentPayload payload = new PiIncidentPayload();
        payload.setIncidentId("pipeline-integration-incident");
        payload.setIdempotencyKey("pipeline-integration-incident");
        payload.setNodeId(node.id);
        payload.setDetectionTime(Instant.parse("2026-11-17T12:00:00Z"));
        payload.setThreatType("WEAPON");
        payload.setThreatSeverity("HIGH");
        payload.setThreatDescription("Detected gun, knife");
        payload.setConfidence(0.94);
        payload.setModelVersion("awarespan-weapon-yolov8n:1.0.0");
        payload.setTier(1);
        payload.setDetectionDomain("WEAPON");
        payload.setDetections(List.of(
                Map.of("label", "gun", "confidence", 0.94),
                Map.of("label", "knife", "confidence", 0.82)));
        payload.setDeterrence(Map.of(
                "attempted", true, "started", true, "reason", "warning_emitted",
                "frequencyHz", 987, "durationSeconds", 1.5));

        ingestionService.ingestIncident(payload);

        assertTrue(incidentRepository.existsById("pipeline-integration-incident"));
        var storedDetection = detectionRepository.findByIncidentId("pipeline-integration-incident");
        assertTrue(storedDetection.isPresent());
        assertEquals("gun,knife", storedDetection.get().predictedClass);
        assertTrue(storedDetection.get().rawScores.contains("\"knife\""));
        assertEquals(1, responseEventRepository.findByIncidentIdOrderByTimestampDesc(
                "pipeline-integration-incident").size());

        IncidentDto androidDto = incidentService.getIncident("pipeline-integration-incident");
        assertEquals("gun,knife", androidDto.detection.predictedClass);
        assertEquals(0.94, androidDto.detection.confidence, 0.0001);
        assertEquals(1, androidDto.responseEvents.size());
        assertTrue(androidDto.responseEvents.get(0).result.contains("STARTED"));
        assertTrue(androidDto.responseEvents.get(0).result.contains("987Hz"));
    }
    @Test
    void telegramTestIncidentStoresAnnotatedImageAndReturnsAndroidEvidenceUrl() {
        Node node = new Node();
        node.id = "telegram-test-node";
        node.name = "Telegram test node";
        node.status = Node.NodeStatus.ONLINE;
        node.deviceSecret = "integration-secret";
        nodeRepository.save(node);

        PiIncidentPayload payload = new PiIncidentPayload();
        payload.setIncidentId("telegram-test-incident-01");
        payload.setIdempotencyKey("telegram-test-incident-01");
        payload.setNodeId(node.id);
        payload.setDetectionTime(Instant.parse("2026-11-20T03:00:00Z"));
        payload.setThreatType("WEAPON");
        payload.setThreatSeverity("HIGH");
        payload.setThreatDescription("TEST — submitted through the Telegram bot; not a live sensor event. Detected gun");
        payload.setConfidence(0.91);
        payload.setModelVersion("telegram-test:yolov8n_best_weapon.pt");
        payload.setTier(2);
        payload.setDetectionDomain("WEAPON");
        payload.setDetections(List.of(Map.of("label", "gun", "confidence", 0.91)));

        // A valid minimal JPEG signature followed by payload bytes is sufficient for MIME validation.
        byte[] jpeg = new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x01, 0x02, 0x03};
        ingestionService.ingestIncidentWithEvidence(payload, jpeg, "image/jpeg", node.id);

        IncidentDto androidDto = incidentService.getIncident(payload.getIncidentId());
        assertEquals(1, androidDto.detail.evidence.size());
        assertEquals("/api/v1/evidence/telegram-test-incident-01/image", androidDto.detail.evidence.get(0).imageUrl);
        assertEquals("UPLOADED", androidDto.detail.evidence.get(0).uploadStatus);
        var media = evidenceService.getEvidenceMedia(payload.getIncidentId(), Evidence.EvidenceType.IMAGE);
        assertEquals(200, media.getStatusCode().value());
        assertEquals(jpeg.length, media.getHeaders().getContentLength());
        assertArrayEquals(jpeg, ((ByteArrayResource) media.getBody()).getByteArray());
        assertTrue(androidDto.threatDescription.startsWith("TEST — submitted through the Telegram bot"));
    }

}
