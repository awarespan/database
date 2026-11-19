package com.sip.backend.service;

import com.sip.backend.dto.NodeEnrollmentRequest;
import com.sip.backend.dto.NodeEnrollmentResponse;
import com.sip.backend.entity.Node;
import com.sip.backend.repository.NodeHeartbeatRepository;
import com.sip.backend.repository.NodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NodeEnrollmentTest {
    @Mock NodeRepository nodes;
    @Mock NodeHeartbeatRepository heartbeats;
    @InjectMocks NodeService service;

    @Test
    void backendAssignsNodeIdNameAndDeviceSecret() {
        when(nodes.findByInstallationId("123e4567-e89b-12d3-a456-426614174000"))
                .thenReturn(Optional.empty());
        when(nodes.save(any(Node.class))).thenAnswer(invocation -> invocation.getArgument(0));
        NodeEnrollmentRequest request = new NodeEnrollmentRequest();
        request.setInstallationId("123e4567-e89b-12d3-a456-426614174000");

        NodeEnrollmentResponse response = service.enroll(request);

        assertTrue(response.nodeId.startsWith("node-"));
        assertTrue(response.name.startsWith("AWARESPAN Edge Node "));
        assertEquals(64, response.deviceSecret.length());
        assertEquals("UNKNOWN", response.status);
        verify(nodes).save(any(Node.class));
    }

    @Test
    void enrollmentIsIdempotentForSameInstallation() {
        Node existing = new Node();
        existing.id = "node-server-issued";
        existing.name = "AWARESPAN Edge Node server";
        existing.installationId = "123e4567-e89b-12d3-a456-426614174000";
        existing.deviceSecret = "a".repeat(64);
        existing.status = Node.NodeStatus.ONLINE;
        when(nodes.findByInstallationId(existing.installationId)).thenReturn(Optional.of(existing));
        NodeEnrollmentRequest request = new NodeEnrollmentRequest();
        request.setInstallationId(existing.installationId);

        NodeEnrollmentResponse response = service.enroll(request);

        assertEquals(existing.id, response.nodeId);
        assertEquals(existing.name, response.name);
        assertEquals(existing.deviceSecret, response.deviceSecret);
        verify(nodes, never()).save(any(Node.class));
    }

    @Test
    void heartbeatCannotAutoRegisterAnUnknownNode() {
        when(nodes.findById("invented-by-node")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service.recordHeartbeat("invented-by-node", new com.sip.backend.dto.NodeHeartbeatPayload()));
        verify(nodes, never()).save(any(Node.class));
    }
}
