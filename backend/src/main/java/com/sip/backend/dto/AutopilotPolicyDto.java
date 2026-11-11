package com.sip.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;

public class AutopilotPolicyDto {
    public String nodeId;
    public Boolean enabled;
    public List<String> allowedThreatTypes;
    public Double confidenceThreshold;
    public Integer deterrenceTimeoutSeconds;
    public Boolean autoEscalateOnDeterrenceFailure;
    public Integer maxDeterrenceAttempts;
    public List<String> alwaysEscalateTypes;
    public Boolean requireMinimumConfidence;
    public Boolean requireMultiModalConfirmation;
    public List<String> neverAutonomousTypes;
    public String syncState;
    @JsonProperty("lastSyncTimestamp")
    public OffsetDateTime lastSync;

    public AutopilotPolicyDto() {}
}