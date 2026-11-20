package com.sip.backend.evidence;

import com.sip.backend.common.ResourceNotFoundException;
import com.sip.backend.entity.Evidence;
import com.sip.backend.entity.Incident;
import com.sip.backend.repository.EvidenceRepository;
import com.sip.backend.repository.IncidentRepository;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class EvidenceService {

    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;
    private final EvidenceRepository evidenceRepository;
    private final IncidentRepository incidentRepository;

    public EvidenceService(EvidenceRepository evidenceRepository, IncidentRepository incidentRepository) {
        this.evidenceRepository = evidenceRepository;
        this.incidentRepository = incidentRepository;
    }

    @Transactional
    public Evidence uploadImage(String incidentId, byte[] bytes, String declaredMimeType, String nodeId) {
        if (bytes == null || bytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is empty");
        }
        if (bytes.length > MAX_IMAGE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Image exceeds 10 MB limit");
        }
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("INCIDENT_NOT_FOUND", "Incident not found: " + incidentId));
        if (incident.node == null || nodeId == null || !incident.node.id.equals(nodeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Node may upload evidence only for its own incident");
        }

        String mimeType = detectMimeType(bytes);
        if (mimeType == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only JPEG, PNG, and WebP images are accepted");
        }
        if (declaredMimeType != null && !declaredMimeType.isBlank()
                && !declaredMimeType.equalsIgnoreCase("application/octet-stream")
                && !declaredMimeType.toLowerCase().startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Uploaded file is not an image");
        }

        String sha256 = sha256(bytes);
        var existing = evidenceRepository.findFirstByIncidentIdAndTypeOrderByCreatedAtDesc(
                incidentId, Evidence.EvidenceType.IMAGE);
        if (existing.isPresent() && sha256.equals(existing.get().sha256)) {
            return existing.get();
        }

        Evidence evidence = new Evidence();
        evidence.id = UUID.randomUUID().toString();
        evidence.incident = incident;
        evidence.type = Evidence.EvidenceType.IMAGE;
        evidence.storageKey = "postgres://evidence/" + evidence.id;
        evidence.mimeType = mimeType;
        evidence.sizeBytes = (long) bytes.length;
        evidence.sha256 = sha256;
        evidence.captureTimestamp = OffsetDateTime.now();
        evidence.uploadStatus = Evidence.UploadStatus.UPLOADED;
        evidence.mediaData = bytes.clone();
        return evidenceRepository.save(evidence);
    }

    @Transactional(readOnly = true)
    public java.util.List<Evidence> getEvidenceForIncident(String incidentId) {
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
        Evidence evidence = evidenceRepository.findFirstByIncidentIdAndTypeOrderByCreatedAtDesc(incidentId, type)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence not found for incident " + incidentId + " and type " + type));
        if (evidence.mediaData == null || evidence.mediaData.length == 0) {
            throw new ResourceNotFoundException("Evidence bytes are not available for incident " + incidentId);
        }
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(evidence.mimeType);
        } catch (Exception ignored) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        String extension = switch (mediaType.toString()) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
        ByteArrayResource resource = new ByteArrayResource(evidence.mediaData);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(evidence.mediaData.length)
                .cacheControl(CacheControl.noCache().cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + evidence.id + "." + extension + "\"")
                .body(resource);
    }

    private String detectMimeType(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) return "image/jpeg";
        if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47) return "image/png";
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return "image/webp";
        return null;
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
