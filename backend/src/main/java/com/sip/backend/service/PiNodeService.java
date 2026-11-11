package com.sip.backend.service;

import com.sip.backend.common.ResourceNotFoundException;
import com.sip.backend.dto.NodeDto;
import com.sip.backend.entity.Node;
import com.sip.backend.repository.NodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;

@Service("piNodeService")
public class PiNodeService {

    private final NodeRepository nodeRepository;

    public PiNodeService(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    @Transactional(readOnly = true)
    public List<NodeDto> getAllNodes() {
        return nodeRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public NodeDto getNode(String id) {
        Node node = nodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NODE_NOT_FOUND",
                    "Node not found: " + id));
        return toDto(node);
    }

    public NodeDto toDto(Node node) {
        if (node == null) return null;
        NodeDto dto = new NodeDto();
        dto.id = node.id;
        dto.name = node.name;
        dto.status = node.status != null ? node.status.name() : "UNKNOWN";
        dto.latitude = node.latitude;
        dto.longitude = node.longitude;
        dto.lastHeartbeat = node.lastHeartbeat;
        dto.batteryLevel = node.batteryLevel;
        dto.firmwareVersion = node.firmwareVersion;
        dto.createdAt = node.createdAt;
        dto.updatedAt = node.updatedAt;
        return dto;
    }
}