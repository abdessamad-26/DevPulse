package com.devpulse.controller;

import com.devpulse.dto.ChaosRequest;
import com.devpulse.entity.ChaosSimulation;
import com.devpulse.service.ChaosService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/chaos")
public class ChaosController {

    private final ChaosService chaosService;

    public ChaosController(ChaosService chaosService) {
        this.chaosService = chaosService;
    }

    @PostMapping
    public ResponseEntity<ChaosSimulation> trigger(@PathVariable Long projectId,
                                                    @Valid @RequestBody ChaosRequest request,
                                                    Authentication authentication) {
        ChaosSimulation simulation = chaosService.trigger(projectId, authentication, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(simulation);
    }

    @GetMapping
    public ResponseEntity<List<ChaosSimulation>> history(@PathVariable Long projectId, Authentication authentication) {
        return ResponseEntity.ok(chaosService.history(projectId, authentication));
    }
}
