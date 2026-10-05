package com.devpulse.controller;

import com.devpulse.dto.AlertRuleRequest;
import com.devpulse.dto.AlertRuleResponse;
import com.devpulse.dto.PageResponse;
import com.devpulse.entity.AlertRule;
import com.devpulse.service.AlertRuleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/alert-rules")
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    public AlertRuleController(AlertRuleService alertRuleService) {
        this.alertRuleService = alertRuleService;
    }

    @PostMapping
    public ResponseEntity<AlertRuleResponse> create(@PathVariable Long projectId,
                                             @Valid @RequestBody AlertRuleRequest request,
                                             Authentication authentication) {
        AlertRule rule = alertRuleService.createRule(projectId, authentication, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AlertRuleResponse.from(rule));
    }

    @GetMapping
    public ResponseEntity<PageResponse<AlertRuleResponse>> list(@PathVariable Long projectId,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "20") int size,
                                                          Authentication authentication) {
        return ResponseEntity.ok(PageResponse.from(
                alertRuleService.listRules(projectId, authentication, page, size).map(AlertRuleResponse::from)));
    }
}
