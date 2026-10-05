package com.devpulse.controller;

import com.devpulse.dto.IncidentAnalysisRequest;
import com.devpulse.dto.IncidentAnalysisResponse;
import com.devpulse.service.AiAnalysisService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    public AiAnalysisController(AiAnalysisService aiAnalysisService) {
        this.aiAnalysisService = aiAnalysisService;
    }

    @PostMapping("/incidents/analyze")
    public IncidentAnalysisResponse analyzeIncident(@Valid @RequestBody IncidentAnalysisRequest request) {
        return aiAnalysisService.analyzeIncident(request);
    }
}
