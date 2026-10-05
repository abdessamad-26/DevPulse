package com.devpulse.dto;

import com.devpulse.entity.ServiceEntity;

import java.time.LocalDateTime;

public record ServiceResponse(
        Long id,
        Long projectId,
        String name,
        String type,
        String healthStatus,
        LocalDateTime createdAt) {

    public static ServiceResponse from(ServiceEntity service) {
        return new ServiceResponse(service.getId(), service.getProjectId(), service.getName(),
                service.getType(), service.getHealthStatus(), service.getCreatedAt());
    }
}
