package com.devpulse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AlertRuleRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Metric is required")
    private String metric;

    @NotBlank(message = "Operator is required (>, <, >=, <=, ==)")
    private String operator;

    @NotNull(message = "Threshold is required")
    private Double threshold;

    private String duration;

    @NotBlank(message = "Severity is required (LOW, MEDIUM, HIGH or CRITICAL)")
    private String severity;

    private Boolean enabled;

    public AlertRuleRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public Double getThreshold() { return threshold; }
    public void setThreshold(Double threshold) { this.threshold = threshold; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
