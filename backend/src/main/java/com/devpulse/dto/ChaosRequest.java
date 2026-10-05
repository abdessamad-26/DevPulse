package com.devpulse.dto;

import jakarta.validation.constraints.NotBlank;

public class ChaosRequest {

    @NotBlank(message = "action is required (KILL_POD, CPU_LOAD, LATENCY, HTTP_500 or DB_FAILURE)")
    private String action;

    private String targetService;

    public ChaosRequest() {}

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTargetService() { return targetService; }
    public void setTargetService(String targetService) { this.targetService = targetService; }
}
