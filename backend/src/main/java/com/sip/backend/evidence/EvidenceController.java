package com.sip.backend.evidence;

import com.sip.backend.entity.Evidence;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/evidence")
public class EvidenceController {

    private final EvidenceService evidenceService;

    public EvidenceController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
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