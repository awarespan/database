package com.sip.backend.dto;

public class NodeEnrollmentResponse {
    public final String nodeId;
    public final String name;
    public final String deviceSecret;
    public final String status;

    public NodeEnrollmentResponse(String nodeId, String name, String deviceSecret, String status) {
        this.nodeId = nodeId;
        this.name = name;
        this.deviceSecret = deviceSecret;
        this.status = status;
    }
}
