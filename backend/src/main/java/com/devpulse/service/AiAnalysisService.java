package com.devpulse.service;

import com.devpulse.dto.IncidentAnalysisRequest;
import com.devpulse.dto.IncidentAnalysisResponse;
import com.devpulse.exception.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

@Service
public class AiAnalysisService {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AiAnalysisService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${ai.service.url:http://localhost:8000}") String aiServiceUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .baseUrl(aiServiceUrl)
                .build();
        this.objectMapper = objectMapper;
    }

    public IncidentAnalysisResponse analyzeIncident(IncidentAnalysisRequest request) {
        IncidentAnalysisRequest payload = new IncidentAnalysisRequest(
                request.title(),
                request.description(),
                request.severity() == null ? "MEDIUM" : request.severity(),
                request.service() == null ? "unknown" : request.service(),
                request.recentLogs() == null ? java.util.List.of() : request.recentLogs());
        try {
            byte[] requestBody = objectMapper.writeValueAsBytes(payload);
            return restClient.post()
                    .uri("/api/analysis/incidents")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(IncidentAnalysisResponse.class);
        } catch (JsonProcessingException | RestClientException ex) {
            throw new ApiException("AI analysis service is unavailable", HttpStatus.BAD_GATEWAY);
        }
    }
}
