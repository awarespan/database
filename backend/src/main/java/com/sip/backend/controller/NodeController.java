package com.sip.backend.controller;

import com.sip.backend.dto.NodeDto;
import com.sip.backend.dto.NodeEnrollmentRequest;
import com.sip.backend.dto.NodeEnrollmentResponse;
import com.sip.backend.dto.NodeHeartbeatPayload;
import com.sip.backend.entity.Node;
import com.sip.backend.entity.NodeHeartbeat;
import com.sip.backend.service.PiNodeService;
import com.sip.backend.repository.NodeRepository;
import com.sip.backend.service.NodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/nodes")
public class NodeController {

    private final NodeService nodeService;
    private final NodeRepository nodeRepository;
    private final PiNodeService piNodeService;

    public NodeController(NodeService nodeService, NodeRepository nodeRepository, PiNodeService piNodeService) {
        this.nodeService = nodeService;
        this.nodeRepository = nodeRepository;
        this.piNodeService = piNodeService;
    }

    @GetMapping
    public ResponseEntity<List<NodeDto>> listNodes() {
        // Never serialize Node entities here: they contain installation IDs and device secrets.
        return ResponseEntity.ok(piNodeService.getAllNodes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NodeDto> getNode(@PathVariable String id) {
        return ResponseEntity.ok(piNodeService.getNode(id));
    }

    @PostMapping("/enroll")
    public ResponseEntity<NodeEnrollmentResponse> enrollNode(@RequestBody NodeEnrollmentRequest request) {
        return ResponseEntity.ok(nodeService.enroll(request));
    }

    @PostMapping("/{id}/heartbeat")
    public ResponseEntity<?> heartbeat(@PathVariable String id, @RequestBody NodeHeartbeatPayload payload) {
        try {
            payload.setNodeId(id);
            NodeHeartbeat hb = nodeService.recordHeartbeat(id, payload);
            return ResponseEntity.ok(hb);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String id, @RequestParam String status) {
        return nodeService.findById(id).map(node -> {
            try {
                node.status = Node.NodeStatus.valueOf(status.toUpperCase());
                nodeRepository.save(node);
                return ResponseEntity.ok(piNodeService.toDto(node));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }).orElse(ResponseEntity.notFound().build());
    }
}