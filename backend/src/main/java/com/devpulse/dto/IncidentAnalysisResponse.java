package com.devpulse.dto;

import java.util.List;

public record IncidentAnalysisResponse(
        String classification,
        double confidence,
        String rootCause,
        List<String> recommendations,
        List<String> signals
) {
}
