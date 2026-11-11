package com.sip.backend.controller;

import com.sip.backend.dto.NodeDto;
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
    public ResponseEntity<?> listNodes(@RequestParam(required = false) String format) {
        if ("dto".equalsIgnoreCase(format)) {
            return ResponseEntity.ok(piNodeService.getAllNodes());
        }
        return ResponseEntity.ok(nodeRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getNode(@PathVariable String id, @RequestParam(required = false) String format) {
        if ("dto".equalsIgnoreCase(format)) {
            return ResponseEntity.ok(piNodeService.getNode(id));
        }
        return nodeService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> registerNode(@RequestBody Node node) {
        if (node.id == null || node.id.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (nodeRepository.existsById(node.id)) {
            return ResponseEntity.status(409).body("Node already registered");
        }
        Node saved = nodeRepository.save(node);
        return ResponseEntity.status(201).body(saved);
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
                return ResponseEntity.ok(node);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }).orElse(ResponseEntity.notFound().build());
    }
}