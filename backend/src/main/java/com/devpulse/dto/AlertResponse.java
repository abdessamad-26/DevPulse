package com.devpulse.dto;

import com.devpulse.entity.Alert;

import java.time.LocalDateTime;

public record AlertResponse(
        Long id,
        String message,
        String severity,
        String status,
        LocalDateTime createdAt) {

    public static AlertResponse from(Alert alert) {
        return new AlertResponse(alert.getId(), alert.getMessage(), alert.getSeverity(),
                alert.getStatus(), alert.getCreatedAt());
    }
}
