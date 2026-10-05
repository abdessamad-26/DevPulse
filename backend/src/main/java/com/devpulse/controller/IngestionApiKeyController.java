package com.devpulse.controller;

import com.devpulse.dto.CreatedIngestionApiKeyResponse;
import com.devpulse.dto.IngestionApiKeyRequest;
import com.devpulse.dto.IngestionApiKeyResponse;
import com.devpulse.service.IngestionApiKeyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/ingestion-keys")
public class IngestionApiKeyController {

    private final IngestionApiKeyService apiKeyService;

    public IngestionApiKeyController(IngestionApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping
    public ResponseEntity<List<IngestionApiKeyResponse>> list(@PathVariable Long projectId,
                                                                Authentication authentication) {
        return ResponseEntity.ok(apiKeyService.list(projectId, authentication));
    }

    @PostMapping
    public ResponseEntity<CreatedIngestionApiKeyResponse> create(
            @PathVariable Long projectId, @Valid @RequestBody IngestionApiKeyRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiKeyService.create(projectId, authentication, request));
    }

    @DeleteMapping("/{keyId}")
    public ResponseEntity<Void> revoke(@PathVariable Long projectId, @PathVariable Long keyId,
                                       Authentication authentication) {
        apiKeyService.revoke(projectId, keyId, authentication);
        return ResponseEntity.noContent().build();
    }
}
