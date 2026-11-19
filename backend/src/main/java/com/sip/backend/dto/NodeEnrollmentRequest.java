package com.sip.backend.dto;

/** Installation-scoped idempotency key; the server owns the public node ID and name. */
public class NodeEnrollmentRequest {
    private String installationId;

    public String getInstallationId() { return installationId; }
    public void setInstallationId(String installationId) { this.installationId = installationId; }
}
