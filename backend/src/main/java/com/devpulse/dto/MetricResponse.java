package com.devpulse.dto;

import com.devpulse.entity.Metric;

import java.time.LocalDateTime;

public record MetricResponse(
        Long id,
        Long projectId,
        String serviceName,
        String metricName,
        Double metricValue,
        String unit,
        LocalDateTime capturedAt) {

    public static MetricResponse from(Metric metric) {
        return new MetricResponse(metric.getId(), metric.getProjectId(), metric.getServiceName(),
                metric.getMetricName(), metric.getMetricValue(), metric.getUnit(), metric.getCapturedAt());
    }
}
