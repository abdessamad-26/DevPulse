package com.devpulse.controller;

import com.devpulse.dto.ChaosRequest;
import com.devpulse.dto.ChaosSimulationResponse;
import com.devpulse.dto.PageResponse;
import com.devpulse.entity.ChaosSimulation;
import com.devpulse.service.ChaosService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/chaos")
public class ChaosController {

    private final ChaosService chaosService;

    public ChaosController(ChaosService chaosService) {
        this.chaosService = chaosService;
    }

    @PostMapping
    public ResponseEntity<ChaosSimulationResponse> trigger(@PathVariable Long projectId,
                                                    @Valid @RequestBody ChaosRequest request,
                                                    Authentication authentication) {
        ChaosSimulation simulation = chaosService.trigger(projectId, authentication, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ChaosSimulationResponse.from(simulation));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ChaosSimulationResponse>> history(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return ResponseEntity.ok(PageResponse.from(
                chaosService.history(projectId, authentication, page, size).map(ChaosSimulationResponse::from)));
    }
}
