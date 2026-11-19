package com.sip.backend.service;

import com.sip.backend.dto.NodeHeartbeatPayload;
import com.sip.backend.dto.NodeEnrollmentRequest;
import com.sip.backend.dto.NodeEnrollmentResponse;
import com.sip.backend.entity.Node;
import com.sip.backend.entity.NodeHeartbeat;
import com.sip.backend.repository.NodeHeartbeatRepository;
import com.sip.backend.repository.NodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.security.SecureRandom;
import java.util.HexFormat;

@Service
public class NodeService {

    private static final Logger log = LoggerFactory.getLogger(NodeService.class);

    private final NodeRepository nodeRepository;
    private final NodeHeartbeatRepository heartbeatRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public NodeService(NodeRepository nodeRepository, NodeHeartbeatRepository heartbeatRepository) {
        this.nodeRepository = nodeRepository;
        this.heartbeatRepository = heartbeatRepository;
    }


    @Transactional
    public NodeEnrollmentResponse enroll(NodeEnrollmentRequest request) {
        String installationId = request != null ? request.getInstallationId() : null;
        try {
            installationId = UUID.fromString(installationId == null ? "" : installationId).toString();
        } catch (IllegalArgumentException invalidUuid) {
            throw new IllegalArgumentException("A valid installationId UUID is required");
        }
        Node node = nodeRepository.findByInstallationId(installationId).orElse(null);
        if (node == null) {
            node = new Node();
            node.id = "node-" + UUID.randomUUID();
            node.name = "AWARESPAN Edge Node " + node.id.substring(5, 13);
            node.installationId = installationId;
            byte[] secret = new byte[32];
            secureRandom.nextBytes(secret);
            node.deviceSecret = HexFormat.of().formatHex(secret);
            node.status = Node.NodeStatus.UNKNOWN;
            node = nodeRepository.save(node);
        }
        return new NodeEnrollmentResponse(node.id, node.name, node.deviceSecret, node.status.name());
    }

    @Transactional
    public NodeHeartbeat recordHeartbeat(String nodeId, NodeHeartbeatPayload payload) {
        Node node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Node is not enrolled: " + nodeId));

        OffsetDateTime ts = payload.getTimestamp() != null
                ? payload.getTimestamp().atOffset(ZoneOffset.UTC)
                : OffsetDateTime.now();
        node.lastHeartbeat = ts;
        node.status = Node.NodeStatus.ONLINE;
        if (payload.getBatteryLevel() != null) node.batteryLevel = payload.getBatteryLevel();
        nodeRepository.save(node);

        // Record heartbeat
        NodeHeartbeat heartbeat = new NodeHeartbeat();
        heartbeat.setId(UUID.randomUUID().toString());
        heartbeat.setNodeId(nodeId);
        heartbeat.setTimestamp(payload.getTimestamp() != null ? payload.getTimestamp() : Instant.now());
        heartbeat.setBatteryLevel(payload.getBatteryLevel());
        heartbeat.setTemperature(payload.getTemperature());
        heartbeat.setStorageAvailableMb(payload.getStorageAvailableMb());
        heartbeat.setModelVersion(payload.getModelVersion());
        heartbeat.setQueueDepth(payload.getQueueDepth() != null ? payload.getQueueDepth() : 0);

        heartbeat = heartbeatRepository.save(heartbeat);
        log.debug("Heartbeat recorded for node {} (queue_depth={})", nodeId, heartbeat.getQueueDepth());
        return heartbeat;
    }

    @Transactional
    public void markNodeOffline(String nodeId) {
        nodeRepository.findById(nodeId).ifPresent(node -> {
            node.status = Node.NodeStatus.OFFLINE;
            nodeRepository.save(node);
            log.info("Node {} marked OFFLINE", nodeId);
        });
    }

    public Optional<Node> findById(String nodeId) {
        return nodeRepository.findById(nodeId);
    }
}