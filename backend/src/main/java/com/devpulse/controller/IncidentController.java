package com.devpulse.controller;

import com.devpulse.dto.IncidentCreateRequest;
import com.devpulse.dto.IncidentDeploymentCorrelationResponse;
import com.devpulse.dto.IncidentUpdateRequest;
import com.devpulse.dto.IncidentResponse;
import com.devpulse.dto.PageResponse;
import com.devpulse.entity.Incident;
import com.devpulse.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Manual/administrative incident endpoints. Automated incident creation
 * (from the AI/anomaly-detection pipeline) will reuse {@link IncidentService}
 * directly once that pipeline exists - see docs/architecture.md#16.
 */
@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    public ResponseEntity<IncidentResponse> create(@Valid @RequestBody IncidentCreateRequest request, Authentication authentication) {
        Incident incident = incidentService.createIncident(authentication, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(IncidentResponse.from(incident));
    }

    @GetMapping
    public ResponseEntity<PageResponse<IncidentResponse>> listForProject(
            @RequestParam Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return ResponseEntity.ok(PageResponse.from(
                incidentService.listIncidentsForProject(projectId, authentication, page, size).map(IncidentResponse::from)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponse> get(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(IncidentResponse.from(incidentService.getIncident(id, authentication)));
    }

    @GetMapping("/{id}/correlated-deployments")
    public ResponseEntity<IncidentDeploymentCorrelationResponse> correlateDeployments(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(incidentService.correlateDeployments(id, authentication));
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<IncidentResponse> analyze(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(IncidentResponse.from(incidentService.analyzeIncident(id, authentication)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<IncidentResponse> update(@PathVariable Long id,
                                            @Valid @RequestBody IncidentUpdateRequest request,
                                            Authentication authentication) {
        return ResponseEntity.ok(IncidentResponse.from(incidentService.updateIncident(id, authentication, request)));
    }
}
