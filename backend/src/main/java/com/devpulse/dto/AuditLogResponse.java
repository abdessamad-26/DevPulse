package com.devpulse.dto;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        Long projectId,
        Long actorUserId,
        String action,
        String entityType,
        Long entityId,
        String details,
        LocalDateTime createdAt
) {
}
