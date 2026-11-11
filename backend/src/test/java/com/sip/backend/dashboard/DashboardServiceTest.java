package com.sip.backend.dashboard;

import com.sip.backend.entity.Incident;
import com.sip.backend.entity.Node;
import com.sip.backend.incident.IncidentMapper;
import com.sip.backend.repository.IncidentRepository;
import com.sip.backend.repository.NodeRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DashboardServiceTest {

    @Test
    void dashboardRequestsOnlyTenMostRecentIncidents() {
        NodeRepository nodes = mock(NodeRepository.class);
        IncidentRepository incidents = mock(IncidentRepository.class);
        IncidentMapper mapper = mock(IncidentMapper.class);
        when(nodes.count()).thenReturn(1L);
        when(nodes.countByStatus(Node.NodeStatus.ONLINE)).thenReturn(1L);
        when(incidents.countByState(any(Incident.IncidentState.class))).thenReturn(0L);
        when(incidents.findTop10ByOrderByCreatedAtDesc()).thenReturn(java.util.List.of());

        var dashboard = new DashboardService(nodes, incidents, mapper).getDashboard();

        assertEquals(1, dashboard.totalNodes);
        assertEquals(1, dashboard.onlineNodes);
        assertEquals(0, dashboard.recentIncidents.size());
        verify(incidents).findTop10ByOrderByCreatedAtDesc();
    }

    @Test
    void activeThreatCountIncludesEscalatedIncidents() {
        NodeRepository nodes = mock(NodeRepository.class);
        IncidentRepository incidents = mock(IncidentRepository.class);
        IncidentMapper mapper = mock(IncidentMapper.class);
        when(nodes.count()).thenReturn(1L);
        when(nodes.countByStatus(Node.NodeStatus.ONLINE)).thenReturn(1L);
        when(incidents.countByState(any(Incident.IncidentState.class))).thenReturn(0L);
        when(incidents.countByState(Incident.IncidentState.ESCALATED)).thenReturn(2L);
        when(incidents.findTop10ByOrderByCreatedAtDesc()).thenReturn(java.util.List.of());

        var dashboard = new DashboardService(nodes, incidents, mapper).getDashboard();

        assertEquals(2L, dashboard.activeThreats);
        assertEquals("RED", dashboard.systemHealth);
    }
}