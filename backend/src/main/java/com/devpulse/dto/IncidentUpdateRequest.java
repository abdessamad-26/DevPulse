package com.devpulse.dto;

import jakarta.validation.constraints.NotBlank;

public class IncidentUpdateRequest {

    @NotBlank(message = "Status is required (OPEN, INVESTIGATING or RESOLVED)")
    private String status;

    private String rootCause;

    private String recommendations;

    private Double confidenceScore;

    public IncidentUpdateRequest() {}

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRootCause() {
        return rootCause;
    }

    public void setRootCause(String rootCause) {
        this.rootCause = rootCause;
    }

    public String getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(String recommendations) {
        this.recommendations = recommendations;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
}
