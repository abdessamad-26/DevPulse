package com.devpulse.service;

import com.devpulse.dto.IncidentAnalysisRequest;
import com.devpulse.dto.IncidentAnalysisResponse;
import com.devpulse.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Service
public class AiAnalysisService {

    private final RestClient restClient;

    public AiAnalysisService(RestClient.Builder restClientBuilder, @Value("${ai.service.url:http://localhost:8000}") String aiServiceUrl) {
        this.restClient = restClientBuilder.baseUrl(aiServiceUrl).build();
    }

    public IncidentAnalysisResponse analyzeIncident(IncidentAnalysisRequest request) {
        Map<String, Object> payload = Map.of(
            "title", request.title(),
            "description", request.description(),
            "severity", request.severity() == null ? "MEDIUM" : request.severity(),
            "service", request.service() == null ? "unknown" : request.service(),
            "recent_logs", request.recentLogs() == null ? java.util.List.of() : request.recentLogs());
        try {
            return restClient.post()
                    .uri("/api/analysis/incidents")
                    .body(payload)
                    .retrieve()
                    .body(IncidentAnalysisResponse.class);
        } catch (RestClientException ex) {
            throw new ApiException("AI analysis service is unavailable", HttpStatus.BAD_GATEWAY);
        }
    }
}
