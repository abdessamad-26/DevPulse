package com.devpulse.controller;

import com.devpulse.dto.IncidentCreateRequest;
import com.devpulse.dto.IncidentUpdateRequest;
import com.devpulse.entity.Incident;
import com.devpulse.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    public ResponseEntity<Incident> create(@Valid @RequestBody IncidentCreateRequest request, Authentication authentication) {
        Incident incident = incidentService.createIncident(authentication, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(incident);
    }

    @GetMapping
    public ResponseEntity<List<Incident>> listForProject(@RequestParam Long projectId, Authentication authentication) {
        return ResponseEntity.ok(incidentService.listIncidentsForProject(projectId, authentication));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Incident> get(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(incidentService.getIncident(id, authentication));
    }

    @PostMapping("/{id}/analyze")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    public ResponseEntity<Incident> analyze(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(incidentService.analyzeIncident(id, authentication));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    public ResponseEntity<Incident> update(@PathVariable Long id,
                                            @Valid @RequestBody IncidentUpdateRequest request,
                                            Authentication authentication) {
        return ResponseEntity.ok(incidentService.updateIncident(id, authentication, request));
    }
}
