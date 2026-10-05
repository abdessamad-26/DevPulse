package com.devpulse.controller;

import com.devpulse.dto.DeploymentRequest;
import com.devpulse.entity.Deployment;
import com.devpulse.service.DeploymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deployments")
public class DeploymentController {

    private final DeploymentService deploymentService;

    public DeploymentController(DeploymentService deploymentService) {
        this.deploymentService = deploymentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    public ResponseEntity<Deployment> record(@Valid @RequestBody DeploymentRequest request, Authentication authentication) {
        Deployment deployment = deploymentService.recordDeployment(authentication, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(deployment);
    }

    @GetMapping
    public ResponseEntity<List<Deployment>> listForProject(@RequestParam Long projectId, Authentication authentication) {
        return ResponseEntity.ok(deploymentService.listDeployments(projectId, authentication));
    }
}
