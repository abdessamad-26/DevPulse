package com.devpulse.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    private JwtProperties withSecret(String secret) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secret);
        return properties;
    }

    @Test
    void shouldRejectMissingSecret() {
        assertThatThrownBy(() -> withSecret(null).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET is not set");
    }

    @Test
    void shouldRejectBlankSecret() {
        assertThatThrownBy(() -> withSecret("   ").validate())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldRejectTooShortSecret() {
        assertThatThrownBy(() -> withSecret("too-short").validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32");
    }

    @Test
    void shouldRejectSecretThatWasCommittedToTheRepository() {
        assertThatThrownBy(() -> withSecret("devpulse-local-secret-change-me-1234567890").validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("publicly known");
    }

    @Test
    void shouldAcceptStrongSecret() {
        assertThatCode(() -> withSecret("k7Qp2xV9mR4tZ8cB1nW6yH3jL5sD0fGa-random-enough").validate())
                .doesNotThrowAnyException();
    }
}
