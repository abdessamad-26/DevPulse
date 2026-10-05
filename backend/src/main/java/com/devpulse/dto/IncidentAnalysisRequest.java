package com.devpulse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record IncidentAnalysisRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 10000) String description,
        @Size(max = 30) String severity,
        @Size(max = 150) String service,
        @JsonProperty("recent_logs") List<@Size(max = 2000) String> recentLogs
) {
}
