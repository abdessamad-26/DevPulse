package com.devpulse.controller;

import com.devpulse.dto.AuditLogResponse;
import com.devpulse.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/projects/{projectId}/audit-logs")
    public ResponseEntity<Page<AuditLogResponse>> listProject(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return ResponseEntity.ok(auditLogService.listProject(projectId, authentication, page, size));
    }

    @GetMapping("/api/audit-logs")
    public ResponseEntity<Page<AuditLogResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return ResponseEntity.ok(auditLogService.listAll(authentication, page, size));
    }
}
