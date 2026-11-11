package com.sip.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;

public class IncidentDetailDto {
    public List<AnnotationDto> annotations;
    public List<ResponseEventDto> responseEvents;
    public List<EvidenceDto> evidence;

    public IncidentDetailDto() {}
}