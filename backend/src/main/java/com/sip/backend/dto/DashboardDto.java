package com.sip.backend.dto;

import java.util.List;

public class DashboardDto {
    public long totalNodes;
    public long onlineNodes;
    public long activeThreats;
    public List<IncidentDto> recentIncidents;
    public String systemHealth;

    public DashboardDto() {}
}