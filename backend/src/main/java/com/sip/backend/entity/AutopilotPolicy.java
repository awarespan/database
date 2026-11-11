package com.sip.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "autopilot_policies")
public class AutopilotPolicy {
    @Id
    @Column(name = "node_id")
    public String nodeId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "node_id")
    public Node node;

    @Column(nullable = false)
    public Boolean enabled = false;

    @Column(name = "allowed_threat_types", nullable = false, columnDefinition = "jsonb")
    public String allowedThreatTypes = "[]";

    @Column(name = "confidence_threshold", nullable = false)
    public Double confidenceThreshold = 0.85;

    @Column(name = "deterrence_timeout_seconds", nullable = false)
    public Integer deterrenceTimeoutSeconds = 5;

    @Column(name = "auto_escalate_on_deterrence_failure", nullable = false)
    public Boolean autoEscalateOnDeterrenceFailure = true;

    @Column(name = "max_deterrence_attempts", nullable = false)
    public Integer maxDeterrenceAttempts = 1;

    @Column(name = "always_escalate_types", nullable = false, columnDefinition = "jsonb")
    public String alwaysEscalateTypes = "[]";

    @Column(name = "require_minimum_confidence", nullable = false)
    public Boolean requireMinimumConfidence = true;

    @Column(name = "require_multi_modal_confirmation", nullable = false)
    public Boolean requireMultiModalConfirmation = false;

    @Column(name = "never_autonomous_types", nullable = false, columnDefinition = "jsonb")
    public String neverAutonomousTypes = "[]";

    @Column(name = "sync_state", nullable = false)
    @Enumerated(EnumType.STRING)
    public SyncState syncState = SyncState.DISABLED;

    @Column(name = "last_sync")
    public OffsetDateTime lastSync;

    public enum SyncState { SYNCED, PENDING, DISABLED, ERROR }

    @PrePersist
    @PreUpdate
    public void preUpdate() {
        lastSync = OffsetDateTime.now();
    }

    public AutopilotPolicy() {}
}