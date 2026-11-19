package com.sip.backend.controller;

import com.sip.backend.dto.NodeDto;
import com.sip.backend.repository.NodeRepository;
import com.sip.backend.service.NodeService;
import com.sip.backend.service.PiNodeService;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NodeControllerTest {
    @Test
    void listAndDetailReturnSafeDtosRatherThanCredentialBearingEntities() {
        NodeService nodeService = mock(NodeService.class);
        NodeRepository repository = mock(NodeRepository.class);
        PiNodeService dtoService = mock(PiNodeService.class);
        NodeDto dto = new NodeDto();
        dto.id = "node-backend-issued";
        dto.name = "AWARESPAN Edge Node";
        when(dtoService.getAllNodes()).thenReturn(List.of(dto));
        when(dtoService.getNode(dto.id)).thenReturn(dto);

        NodeController controller = new NodeController(nodeService, repository, dtoService);

        assertSame(dto, controller.listNodes().getBody().get(0));
        assertSame(dto, controller.getNode(dto.id).getBody());
        verify(repository, never()).findAll();
        verify(repository, never()).findById(anyString());
    }
}
