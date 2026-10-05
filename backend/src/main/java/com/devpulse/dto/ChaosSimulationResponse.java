package com.devpulse.dto;

import com.devpulse.entity.ChaosSimulation;

import java.time.LocalDateTime;

public record ChaosSimulationResponse(
        Long id,
        Long projectId,
        String action,
        String targetService,
        String triggeredBy,
        String status,
        Long resultingIncidentId,
        LocalDateTime createdAt) {

    public static ChaosSimulationResponse from(ChaosSimulation simulation) {
        return new ChaosSimulationResponse(simulation.getId(), simulation.getProjectId(),
                simulation.getAction(), simulation.getTargetService(), simulation.getTriggeredBy(),
                simulation.getStatus(), simulation.getResultingIncidentId(), simulation.getCreatedAt());
    }
}
