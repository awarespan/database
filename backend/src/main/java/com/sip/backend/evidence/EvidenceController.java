package com.sip.backend.evidence;

import com.sip.backend.dto.EvidenceDto;
import com.sip.backend.entity.Evidence;
import com.sip.backend.incident.IncidentMapper;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/evidence")
public class EvidenceController {

    private final EvidenceService evidenceService;
    private final IncidentMapper mapper;

    public EvidenceController(EvidenceService evidenceService, IncidentMapper mapper) {
        this.evidenceService = evidenceService;
        this.mapper = mapper;
    }

    @PostMapping(value = "/{incidentId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('DEVICE', 'NODE')")
    public ResponseEntity<EvidenceDto> uploadImage(
            @PathVariable String incidentId,
            @RequestPart("file") MultipartFile file,
            Authentication authentication) throws IOException {
        Evidence evidence = evidenceService.uploadImage(
                incidentId, file.getBytes(), file.getContentType(), authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toEvidenceDto(evidence));
    }

    @GetMapping("/{incidentId}/image")
    public ResponseEntity<Resource> getImage(@PathVariable String incidentId) {
        return evidenceService.getEvidenceMedia(incidentId, Evidence.EvidenceType.IMAGE);
    }

    @GetMapping("/{incidentId}/video")
    public ResponseEntity<Resource> getVideo(@PathVariable String incidentId) {
        return evidenceService.getEvidenceMedia(incidentId, Evidence.EvidenceType.VIDEO);
    }

    @GetMapping("/{incidentId}/audio")
    public ResponseEntity<Resource> getAudio(@PathVariable String incidentId) {
        return evidenceService.getEvidenceMedia(incidentId, Evidence.EvidenceType.AUDIO);
    }

    @GetMapping("/{incidentId}/thumbnail")
    public ResponseEntity<Resource> getThumbnail(@PathVariable String incidentId) {
        return evidenceService.getEvidenceMedia(incidentId, Evidence.EvidenceType.THUMBNAIL);
    }
}
