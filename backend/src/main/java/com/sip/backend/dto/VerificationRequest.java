package com.sip.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class VerificationRequest {
    @NotBlank
    public String label;

    public String notes;
    public Double confidence;

    public VerificationRequest() {}
    public VerificationRequest(String label, String notes) {
        this.label = label;
        this.notes = notes;
    }
}