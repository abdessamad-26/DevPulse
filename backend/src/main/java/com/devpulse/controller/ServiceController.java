package com.devpulse.controller;

import com.devpulse.dto.ServiceRequest;
import com.devpulse.entity.ServiceEntity;
import com.devpulse.service.ServiceManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/services")
public class ServiceController {

    private final ServiceManagementService serviceManagementService;

    public ServiceController(ServiceManagementService serviceManagementService) {
        this.serviceManagementService = serviceManagementService;
    }

    @PostMapping
    public ResponseEntity<ServiceEntity> create(@PathVariable Long projectId,
                                                 @Valid @RequestBody ServiceRequest request,
                                                 Authentication authentication) {
        ServiceEntity service = serviceManagementService.createService(projectId, authentication, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(service);
    }

    @GetMapping
    public ResponseEntity<List<ServiceEntity>> list(@PathVariable Long projectId, Authentication authentication) {
        return ResponseEntity.ok(serviceManagementService.listServices(projectId, authentication));
    }
}
