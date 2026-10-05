package com.devpulse.dto;

import jakarta.validation.constraints.NotBlank;

public class ServiceRequest {

    @NotBlank(message = "Service name is required")
    private String name;

    private String type;

    private String healthStatus;

    public ServiceRequest() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }
}
