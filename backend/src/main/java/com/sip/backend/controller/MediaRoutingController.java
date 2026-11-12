package com.sip.backend.controller;

import com.sip.backend.entity.Command;
import com.sip.backend.service.CommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AWARESPAN — Test Media Ingestion & Routing Controller
 * Receives test media (photo, video, audio) from temporary testing frontend,
 * stores it temporarily, and routes it to the designated Raspberry Pi node via Command Queue & MQTT.
 */
@RestController
@RequestMapping("/api/v1/media")
public class MediaRoutingController {

    private static final Logger log = LoggerFactory.getLogger(MediaRoutingController.class);

    private final CommandService commandService;

    // In-memory media storage for active test jobs (max 50 recent items)
    private final Map<String, MediaRecord> mediaStore = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> mediaResults = new ConcurrentHashMap<>();

    public MediaRoutingController(CommandService commandService) {
        this.commandService = commandService;
    }

    public static class MediaRecord {
        public final String id;
        public final String nodeId;
        public final String mediaType;
        public final String contentType;
        public final byte[] data;
        public final long timestamp;

        public MediaRecord(String id, String nodeId, String mediaType, String contentType, byte[] data) {
            this.id = id;
            this.nodeId = nodeId;
            this.mediaType = mediaType;
            this.contentType = contentType;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
    }

    @PostMapping(value = "/upload", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> uploadMultipart(
            @RequestParam String nodeId,
            @RequestParam String mediaType,
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Empty file"));
            }
            byte[] bytes = file.getBytes();
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            return routeMedia(nodeId, mediaType, bytes, contentType, file.getOriginalFilename());
        } catch (IOException e) {
            log.error("Failed to read uploaded file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process uploaded file"));
        }
    }

    @PostMapping(value = "/upload", consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> uploadJson(@RequestBody Map<String, String> payload) {
        String nodeId = payload.get("nodeId");
        String mediaType = payload.get("mediaType");
        String b64Data = payload.get("data");
        String contentType = payload.getOrDefault("contentType", "image/jpeg");
        String filename = payload.getOrDefault("filename", "upload.dat");

        if (nodeId == null || mediaType == null || b64Data == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields: nodeId, mediaType, data"));
        }

        byte[] bytes = Base64.getDecoder().decode(b64Data);
        return routeMedia(nodeId, mediaType, bytes, contentType, filename);
    }

    private ResponseEntity<?> routeMedia(String nodeId, String mediaType, byte[] bytes, String contentType, String filename) {
        String mediaId = "media-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6);
        MediaRecord record = new MediaRecord(mediaId, nodeId, mediaType.toUpperCase(), contentType, bytes);
        mediaStore.put(mediaId, record);

        // Prune old items if exceeds capacity
        if (mediaStore.size() > 50) {
            mediaStore.entrySet().stream()
                    .min(Comparator.comparingLong(e -> e.getValue().timestamp))
                    .ifPresent(e -> mediaStore.remove(e.getKey()));
        }

        Map<String, Object> cmdPayload = new HashMap<>();
        cmdPayload.put("mediaId", mediaId);
        cmdPayload.put("mediaType", mediaType.toUpperCase());
        cmdPayload.put("filename", filename);
        cmdPayload.put("contentType", contentType);
        cmdPayload.put("sizeBytes", bytes.length);

        // If file is reasonably small (< 500KB, e.g. single frame photo), include base64 inline for zero-latency delivery
        if (bytes.length <= 512 * 1024) {
            cmdPayload.put("b64", Base64.getEncoder().encodeToString(bytes));
        }
        cmdPayload.put("downloadUrl", "/api/v1/media/" + mediaId);

        try {
            Command cmd = commandService.dispatchCommand(
                    nodeId,
                    Command.CommandType.MEDIA_INGEST,
                    cmdPayload,
                    10,
                    600 // 10 min TTL
            );

            log.info("Media {} ({}, {} bytes) routed to node {} via command {}",
                    mediaId, mediaType, bytes.length, nodeId, cmd.getId());

            Map<String, Object> resp = new HashMap<>();
            resp.put("mediaId", mediaId);
            resp.put("commandId", cmd.getId());
            resp.put("nodeId", nodeId);
            resp.put("mediaType", mediaType.toUpperCase());
            resp.put("status", "DISPATCHED");
            resp.put("sizeBytes", bytes.length);
            resp.put("createdAt", cmd.getCreatedAt().toString());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(resp);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{mediaId}")
    public ResponseEntity<Resource> getMedia(@PathVariable String mediaId) {
        MediaRecord record = mediaStore.get(mediaId);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        ByteArrayResource resource = new ByteArrayResource(record.data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + record.id + "\"")
                .contentType(MediaType.parseMediaType(record.contentType))
                .contentLength(record.data.length)
                .body(resource);
    }

    @PostMapping("/{mediaId}/result")
    public ResponseEntity<?> recordResult(@PathVariable String mediaId, @RequestBody Map<String, Object> result) {
        mediaResults.put(mediaId, result);
        log.info("Recorded inference result for media {}: {}", mediaId, result);
        return ResponseEntity.ok(Map.of("status", "recorded"));
    }

    @GetMapping("/{mediaId}/result")
    public ResponseEntity<?> getResult(@PathVariable String mediaId) {
        Map<String, Object> result = mediaResults.get(mediaId);
        if (result == null) {
            return ResponseEntity.ok(Map.of("mediaId", mediaId, "status", "PROCESSING"));
        }
        return ResponseEntity.ok(result);
    }
}
