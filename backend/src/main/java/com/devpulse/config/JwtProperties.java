package com.devpulse.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private static final int MIN_SECRET_BYTES = 32;

    /**
     * Secrets that were committed to this repository at some point. They are
     * public, so a server using one of them lets anyone forge valid tokens.
     */
    private static final Set<String> KNOWN_INSECURE_SECRETS = Set.of(
            "devpulse-local-secret-change-me-1234567890",
            "change_me_in_local_env"
    );

    private String secret;
    private long expiration;
    private long refreshExpiration;

    /**
     * Fail fast at startup: a missing, short or publicly known signing secret
     * must never reach a running server (instead of failing on the first login).
     */
    @PostConstruct
    void validate() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET is not set. Define a random secret of at least " + MIN_SECRET_BYTES
                            + " characters, e.g. `openssl rand -base64 48` "
                            + "(or run scripts/dev.ps1 / scripts/dev.sh, which create .env for you).");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least " + MIN_SECRET_BYTES + " characters long.");
        }
        if (KNOWN_INSECURE_SECRETS.contains(secret)) {
            throw new IllegalStateException(
                    "JWT_SECRET uses a publicly known placeholder value. Generate a new random secret.");
        }
    }

    // Explicit getters in case Lombok annotation processing is not available
    public String getSecret() {
        return secret;
    }

    public long getExpiration() {
        return expiration;
    }

    public long getRefreshExpiration() {
        return refreshExpiration;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public void setRefreshExpiration(long refreshExpiration) {
        this.refreshExpiration = refreshExpiration;
    }
}
