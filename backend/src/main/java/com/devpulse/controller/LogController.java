package com.devpulse.controller;

import com.devpulse.dto.LogIngestRequest;
import com.devpulse.dto.LogResponse;
import com.devpulse.dto.PageResponse;
import com.devpulse.service.LogService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/logs")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @PostMapping
    public ResponseEntity<List<LogResponse>> ingest(@PathVariable Long projectId,
                                                     @Valid @RequestBody LogIngestRequest request,
                                                     Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(logService.ingest(projectId, authentication, request).stream()
                        .map(LogResponse::from).toList());
    }

    @GetMapping
    public ResponseEntity<PageResponse<LogResponse>> search(@PathVariable Long projectId,
                                                             @RequestParam(required = false) String service,
                                                             @RequestParam(required = false) String environment,
                                                             @RequestParam(required = false) String level,
                                                             @RequestParam(required = false) String q,
                                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int size,
                                                             Authentication authentication) {
        return ResponseEntity.ok(PageResponse.from(logService.search(projectId, authentication, service, environment,
                level, q, from, to, page, size).map(LogResponse::from)));
    }
}
