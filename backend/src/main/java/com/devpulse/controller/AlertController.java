package com.devpulse.controller;

import com.devpulse.entity.Alert;
import com.devpulse.service.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ResponseEntity<List<Alert>> list(@RequestParam Long projectId, Authentication authentication) {
        return ResponseEntity.ok(alertService.listAlerts(projectId, authentication));
    }

    @PostMapping("/{id}/ack")
    public ResponseEntity<Alert> acknowledge(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(alertService.acknowledge(id, authentication));
    }
}
