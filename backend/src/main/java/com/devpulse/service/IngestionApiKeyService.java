package com.devpulse.service;

import com.devpulse.dto.CreatedIngestionApiKeyResponse;
import com.devpulse.dto.IngestionApiKeyRequest;
import com.devpulse.dto.IngestionApiKeyResponse;
import com.devpulse.entity.IngestionApiKey;
import com.devpulse.entity.Project;
import com.devpulse.entity.User;
import com.devpulse.exception.ApiException;
import com.devpulse.repository.IngestionApiKeyRepository;
import com.devpulse.security.IngestionApiKeyPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
public class IngestionApiKeyService {

    private static final String KEY_PREFIX = "dp_ing_";
    private static final int RANDOM_BYTES = 32;

    private final IngestionApiKeyRepository apiKeyRepository;
    private final ProjectAccessService projectAccessService;
    private final AuditLogService auditLogService;
    private final SecureRandom secureRandom = new SecureRandom();

    public IngestionApiKeyService(IngestionApiKeyRepository apiKeyRepository,
                                  ProjectAccessService projectAccessService,
                                  AuditLogService auditLogService) {
        this.apiKeyRepository = apiKeyRepository;
        this.projectAccessService = projectAccessService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public CreatedIngestionApiKeyResponse create(Long projectId, Authentication authentication,
                                                IngestionApiKeyRequest request) {
        Project project = projectAccessService.requireProjectManager(projectId, authentication);
        User creator = projectAccessService.resolveCurrentUser(authentication);
        String rawKey = generateKey();

        IngestionApiKey apiKey = new IngestionApiKey();
        apiKey.setProject(project);
        apiKey.setCreatedBy(creator);
        apiKey.setName(request.name().trim());
        apiKey.setKeyHash(hash(rawKey));
        apiKey.setKeyPrefix(rawKey.substring(0, 14));
        IngestionApiKey saved = apiKeyRepository.save(apiKey);

        auditLogService.recordProject(projectId, creator, "INGESTION_KEY_CREATED",
                "INGESTION_API_KEY", saved.getId(), null);
        return new CreatedIngestionApiKeyResponse(saved.getId(), saved.getName(), rawKey,
                saved.getKeyPrefix(), saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<IngestionApiKeyResponse> list(Long projectId, Authentication authentication) {
        projectAccessService.requireProjectManager(projectId, authentication);
        return apiKeyRepository.findByProject_IdOrderByCreatedAtDesc(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void revoke(Long projectId, Long keyId, Authentication authentication) {
        projectAccessService.requireProjectManager(projectId, authentication);
        IngestionApiKey key = apiKeyRepository.findByIdAndProject_Id(keyId, projectId)
                .orElseThrow(() -> new ApiException("Ingestion API key not found", HttpStatus.NOT_FOUND));
        apiKeyRepository.delete(key);
        auditLogService.recordProject(projectId, projectAccessService.resolveCurrentUser(authentication),
                "INGESTION_KEY_REVOKED", "INGESTION_API_KEY", keyId, null);
    }

    @Transactional
    public IngestionApiKeyPrincipal authenticate(String rawKey) {
        if (rawKey == null || rawKey.length() > 128 || !rawKey.startsWith(KEY_PREFIX)) {
            return null;
        }
        IngestionApiKey apiKey = apiKeyRepository.findByKeyHash(hash(rawKey)).orElse(null);
        if (apiKey == null) {
            return null;
        }
        apiKey.setLastUsedAt(LocalDateTime.now());
        apiKeyRepository.save(apiKey);
        return new IngestionApiKeyPrincipal(apiKey.getId(), apiKey.getProject().getId(), apiKey.getName());
    }

    private String generateKey() {
        byte[] random = new byte[RANDOM_BYTES];
        secureRandom.nextBytes(random);
        return KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    private String hash(String rawKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private IngestionApiKeyResponse toResponse(IngestionApiKey key) {
        return new IngestionApiKeyResponse(key.getId(), key.getName(), key.getKeyPrefix(),
                key.getCreatedAt(), key.getLastUsedAt());
    }
}
