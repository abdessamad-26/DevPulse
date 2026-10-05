package com.devpulse.controller;

import com.devpulse.dto.MetricIngestRequest;
import com.devpulse.entity.Metric;
import com.devpulse.service.MetricService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/metrics")
public class MetricController {

    private final MetricService metricService;

    public MetricController(MetricService metricService) {
        this.metricService = metricService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    public ResponseEntity<List<Metric>> ingest(@PathVariable Long projectId,
                                                @Valid @RequestBody MetricIngestRequest request,
                                                Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(metricService.ingest(projectId, authentication, request));
    }

    @GetMapping
    public ResponseEntity<List<Metric>> query(@PathVariable Long projectId,
                                               @RequestParam(required = false) String metric,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                               Authentication authentication) {
        return ResponseEntity.ok(metricService.query(projectId, authentication, metric, from, to));
    }
}
