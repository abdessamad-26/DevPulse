package com.devpulse.dto;

import java.time.LocalDateTime;
import java.util.List;

public record IncidentDeploymentCorrelationResponse(
        Long incidentId,
        LocalDateTime incidentTime,
        LocalDateTime windowStart,
        List<DeploymentResponse> deployments) {
}
