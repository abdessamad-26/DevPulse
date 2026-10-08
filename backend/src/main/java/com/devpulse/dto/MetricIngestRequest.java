package com.devpulse.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public class MetricIngestRequest {

    @NotEmpty(message = "At least one metric point is required")
    @Size(max = 1000, message = "A batch can contain at most 1000 metric points")
    @Valid
    private List<Point> points;

    public MetricIngestRequest() {}

    public List<Point> getPoints() { return points; }
    public void setPoints(List<Point> points) { this.points = points; }

    public static class Point {

        // Limits mirror the column sizes in V1__init_schema.sql (metrics table).
        @Size(max = 150, message = "serviceName must be at most 150 characters")
        private String serviceName;

        @NotBlank(message = "metricName is required")
        @Size(max = 150, message = "metricName must be at most 150 characters")
        private String metricName;

        @NotNull(message = "value is required")
        private Double value;

        @Size(max = 50, message = "unit must be at most 50 characters")
        private String unit;

        private LocalDateTime capturedAt;

        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }

        public String getMetricName() { return metricName; }
        public void setMetricName(String metricName) { this.metricName = metricName; }

        public Double getValue() { return value; }
        public void setValue(Double value) { this.value = value; }

        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }

        public LocalDateTime getCapturedAt() { return capturedAt; }
        public void setCapturedAt(LocalDateTime capturedAt) { this.capturedAt = capturedAt; }
    }
}
