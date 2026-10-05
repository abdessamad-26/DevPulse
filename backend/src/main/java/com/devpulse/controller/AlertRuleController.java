package com.devpulse.controller;

import com.devpulse.dto.AlertRuleRequest;
import com.devpulse.entity.AlertRule;
import com.devpulse.service.AlertRuleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/alert-rules")
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    public AlertRuleController(AlertRuleService alertRuleService) {
        this.alertRuleService = alertRuleService;
    }

    @PostMapping
    public ResponseEntity<AlertRule> create(@PathVariable Long projectId,
                                             @Valid @RequestBody AlertRuleRequest request,
                                             Authentication authentication) {
        AlertRule rule = alertRuleService.createRule(projectId, authentication, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(rule);
    }

    @GetMapping
    public ResponseEntity<List<AlertRule>> list(@PathVariable Long projectId, Authentication authentication) {
        return ResponseEntity.ok(alertRuleService.listRules(projectId, authentication));
    }
}
