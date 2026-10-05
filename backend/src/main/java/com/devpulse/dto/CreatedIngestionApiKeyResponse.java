package com.devpulse.dto;

import java.time.LocalDateTime;

public record CreatedIngestionApiKeyResponse(
        Long id,
        String name,
        String key,
        String prefix,
        LocalDateTime createdAt
) {
}
