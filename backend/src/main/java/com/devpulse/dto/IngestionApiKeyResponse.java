package com.devpulse.dto;

import java.time.LocalDateTime;

public record IngestionApiKeyResponse(
        Long id,
        String name,
        String prefix,
        LocalDateTime createdAt,
        LocalDateTime lastUsedAt
) {
}
