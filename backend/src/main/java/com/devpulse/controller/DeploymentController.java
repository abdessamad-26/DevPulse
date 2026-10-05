package com.devpulse.controller;

import com.devpulse.dto.DeploymentRequest;
import com.devpulse.dto.DeploymentResponse;
import com.devpulse.dto.PageResponse;
import com.devpulse.entity.Deployment;
import com.devpulse.service.DeploymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deployments")
public class DeploymentController {

    private final DeploymentService deploymentService;

    public DeploymentController(DeploymentService deploymentService) {
        this.deploymentService = deploymentService;
    }

    @PostMapping
    public ResponseEntity<DeploymentResponse> record(@Valid @RequestBody DeploymentRequest request, Authentication authentication) {
        Deployment deployment = deploymentService.recordDeployment(authentication, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(DeploymentResponse.from(deployment));
    }

    @GetMapping
    public ResponseEntity<PageResponse<DeploymentResponse>> listForProject(
            @RequestParam Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return ResponseEntity.ok(PageResponse.from(
                deploymentService.listDeployments(projectId, authentication, page, size).map(DeploymentResponse::from)));
    }
}
