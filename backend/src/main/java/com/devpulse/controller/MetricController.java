package com.devpulse.controller;

import com.devpulse.dto.MetricIngestRequest;
import com.devpulse.dto.MetricResponse;
import com.devpulse.dto.PageResponse;
import com.devpulse.service.MetricService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<MetricResponse>> ingest(@PathVariable Long projectId,
                                                        @Valid @RequestBody MetricIngestRequest request,
                                                        Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(metricService.ingest(projectId, authentication, request).stream()
                        .map(MetricResponse::from).toList());
    }

    @GetMapping
    public ResponseEntity<PageResponse<MetricResponse>> query(@PathVariable Long projectId,
                                                       @RequestParam(required = false) String metric,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size,
                                                       Authentication authentication) {
        return ResponseEntity.ok(PageResponse.from(
                metricService.query(projectId, authentication, metric, from, to, page, size).map(MetricResponse::from)));
    }
}
