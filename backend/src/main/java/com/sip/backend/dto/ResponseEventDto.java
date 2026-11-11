package com.sip.backend.dto;

import java.time.OffsetDateTime;

public class ResponseEventDto {
    public String id;
    public String incidentId;
    public String actionType;
    public OffsetDateTime timestamp;
    public String result;
    public Boolean autonomous;

    public ResponseEventDto() {}
}