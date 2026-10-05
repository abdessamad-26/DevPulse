package com.devpulse.dto;

public record AlertWebhookPayload(
        String event,
        Long alertId,
        Long projectId,
        Long alertRuleId,
        String ruleName,
        String metric,
        String operator,
        Double threshold,
        Double currentValue,
        String severity,
        String status,
        String message,
        String occurredAt) {
}
