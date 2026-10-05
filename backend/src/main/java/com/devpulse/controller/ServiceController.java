package com.devpulse.controller;

import com.devpulse.dto.ServiceRequest;
import com.devpulse.dto.ServiceResponse;
import com.devpulse.dto.PageResponse;
import com.devpulse.entity.ServiceEntity;
import com.devpulse.service.ServiceManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/services")
public class ServiceController {

    private final ServiceManagementService serviceManagementService;

    public ServiceController(ServiceManagementService serviceManagementService) {
        this.serviceManagementService = serviceManagementService;
    }

    @PostMapping
    public ResponseEntity<ServiceResponse> create(@PathVariable Long projectId,
                                                 @Valid @RequestBody ServiceRequest request,
                                                 Authentication authentication) {
        ServiceEntity service = serviceManagementService.createService(projectId, authentication, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ServiceResponse.from(service));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ServiceResponse>> list(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return ResponseEntity.ok(PageResponse.from(
                serviceManagementService.listServices(projectId, authentication, page, size).map(ServiceResponse::from)));
    }
}
