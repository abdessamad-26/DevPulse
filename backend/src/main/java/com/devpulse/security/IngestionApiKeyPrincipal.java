package com.devpulse.security;

public record IngestionApiKeyPrincipal(Long keyId, Long projectId, String keyName) {
}
