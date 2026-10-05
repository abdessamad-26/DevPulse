package com.devpulse.dto;

import com.devpulse.entity.LogEntry;

import java.time.LocalDateTime;

public record LogResponse(
        Long id,
        Long projectId,
        String serviceName,
        String environment,
        String level,
        String message,
        LocalDateTime timestamp) {

    public static LogResponse from(LogEntry log) {
        return new LogResponse(log.getId(), log.getProjectId(), log.getServiceName(),
                log.getEnvironment(), log.getLevel(), log.getMessage(), log.getTimestamp());
    }
}
