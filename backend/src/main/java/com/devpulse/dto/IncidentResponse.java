package com.devpulse.dto;

import com.devpulse.entity.Incident;

import java.time.LocalDateTime;

public record IncidentResponse(
        Long id,
        Long projectId,
        Long serviceId,
        String title,
        String description,
        String severity,
        String status,
        String serviceName,
        LocalDateTime startedAt,
        LocalDateTime resolvedAt,
        LocalDateTime detectedAt,
        String rootCause,
        String recommendations,
        Double confidenceScore,
        LocalDateTime createdAt) {

    public static IncidentResponse from(Incident incident) {
        return new IncidentResponse(incident.getId(), incident.getProjectId(),
                incident.getServiceRef() == null ? null : incident.getServiceRef().getId(),
                incident.getTitle(), incident.getDescription(), incident.getSeverity(),
                incident.getStatus(), incident.getServiceName(), incident.getStartedAt(),
                incident.getResolvedAt(), incident.getDetectedAt(), incident.getRootCause(),
                incident.getRecommendations(), incident.getConfidenceScore(), incident.getCreatedAt());
    }
}
