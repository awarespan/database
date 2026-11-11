package com.sip.backend.dashboard;

import com.sip.backend.dto.DashboardDto;
import com.sip.backend.entity.Incident;
import com.sip.backend.incident.IncidentMapper;
import com.sip.backend.repository.IncidentRepository;
import com.sip.backend.repository.NodeRepository;
import com.sip.backend.entity.Node;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class DashboardService {

    private static final List<Incident.IncidentState> ACTIVE_STATES = List.of(
            Incident.IncidentState.DETECTED,
            Incident.IncidentState.PENDING_VERIFICATION,
            Incident.IncidentState.VERIFIED,
            Incident.IncidentState.ESCALATED
    );

    private final NodeRepository nodeRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentMapper incidentMapper;

    public DashboardService(NodeRepository nodeRepository, IncidentRepository incidentRepository,
                            IncidentMapper incidentMapper) {
        this.nodeRepository = nodeRepository;
        this.incidentRepository = incidentRepository;
        this.incidentMapper = incidentMapper;
    }

    @Transactional(readOnly = true)
    public DashboardDto getDashboard() {
        DashboardDto dto = new DashboardDto();
        dto.totalNodes = nodeRepository.count();
        dto.onlineNodes = nodeRepository.countByStatus(Node.NodeStatus.ONLINE);
        dto.activeThreats = ACTIVE_STATES.stream()
                .mapToLong(incidentRepository::countByState)
                .sum();

        List<Incident> recent = incidentRepository.findTop10ByOrderByCreatedAtDesc();
        dto.recentIncidents = recent.stream().map(incidentMapper::toDto).toList();

        if (dto.totalNodes == 0 || dto.onlineNodes == 0) {
            dto.systemHealth = "YELLOW";
        } else if (dto.activeThreats > 0) {
            dto.systemHealth = "RED";
        } else {
            dto.systemHealth = "GREEN";
        }

        return dto;
    }
}