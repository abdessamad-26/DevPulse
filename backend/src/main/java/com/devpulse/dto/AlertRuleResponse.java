package com.devpulse.dto;

import com.devpulse.entity.AlertRule;

import java.time.LocalDateTime;

public record AlertRuleResponse(
        Long id,
        Long projectId,
        String name,
        String metric,
        String operator,
        Double threshold,
        String duration,
        String severity,
        boolean enabled,
        LocalDateTime createdAt) {

    public static AlertRuleResponse from(AlertRule rule) {
        return new AlertRuleResponse(rule.getId(), rule.getProjectId(), rule.getName(),
                rule.getMetric(), rule.getOperator(), rule.getThreshold(), rule.getDuration(),
                rule.getSeverity(), rule.isEnabled(), rule.getCreatedAt());
    }
}
