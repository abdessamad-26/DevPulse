package com.devpulse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IngestionApiKeyRequest(
        @NotBlank @Size(max = 80) String name
) {
}
