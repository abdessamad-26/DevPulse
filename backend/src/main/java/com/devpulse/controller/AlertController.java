package com.devpulse.controller;

import com.devpulse.dto.AlertResponse;
import com.devpulse.dto.PageResponse;
import com.devpulse.service.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<AlertResponse>> list(@RequestParam Long projectId,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size,
                                                     Authentication authentication) {
        return ResponseEntity.ok(PageResponse.from(
                alertService.listAlerts(projectId, authentication, page, size).map(AlertResponse::from)));
    }

    @PostMapping("/{id}/ack")
    public ResponseEntity<AlertResponse> acknowledge(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(AlertResponse.from(alertService.acknowledge(id, authentication)));
    }
}
