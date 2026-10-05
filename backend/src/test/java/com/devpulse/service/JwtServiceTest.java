package com.devpulse.service;

import com.devpulse.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-for-jwt-signing-min-32-bytes-long";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setExpiration(3600000L);
        properties.setRefreshExpiration(604800000L);
        jwtService = new JwtService(properties);
    }

    @Test
    void shouldGenerateAndValidateAccessToken() {
        String token = jwtService.generateToken("alice@example.com", "ADMIN");

        assertThat(token).isNotBlank();
        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.isAccessTokenValid(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice@example.com");
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void shouldGenerateValidRefreshToken() {
        String refreshToken = jwtService.generateRefreshToken("bob@example.com");

        assertThat(jwtService.isTokenValid(refreshToken)).isTrue();
        assertThat(jwtService.isRefreshTokenValid(refreshToken)).isTrue();
        assertThat(jwtService.extractUsername(refreshToken)).isEqualTo("bob@example.com");
    }

    @Test
    void refreshTokenMustNotBeAcceptedAsAccessToken() {
        String refreshToken = jwtService.generateRefreshToken("bob@example.com");

        assertThat(jwtService.isAccessTokenValid(refreshToken)).isFalse();
    }

    @Test
    void accessTokenMustNotBeAcceptedAsRefreshToken() {
        String accessToken = jwtService.generateToken("alice@example.com", "ADMIN");

        assertThat(jwtService.isRefreshTokenValid(accessToken)).isFalse();
    }

    @Test
    void shouldRejectCorrectlySignedTokenWithoutTypeClaim() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String legacyToken = Jwts.builder()
                .subject("legacy@example.com")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();

        assertThat(jwtService.isTokenValid(legacyToken)).isTrue();
        assertThat(jwtService.isAccessTokenValid(legacyToken)).isFalse();
        assertThat(jwtService.isRefreshTokenValid(legacyToken)).isFalse();
    }

    @Test
    void shouldRejectMalformedToken() {
        assertThat(jwtService.isTokenValid("not-a-real-token")).isFalse();
        assertThat(jwtService.isAccessTokenValid("not-a-real-token")).isFalse();
        assertThat(jwtService.isRefreshTokenValid("not-a-real-token")).isFalse();
    }

    @Test
    void shouldRejectTokenSignedWithDifferentSecret() {
        JwtProperties otherProperties = new JwtProperties();
        otherProperties.setSecret("a-completely-different-secret-key-32-bytes-plus");
        otherProperties.setExpiration(3600000L);
        otherProperties.setRefreshExpiration(604800000L);
        JwtService otherService = new JwtService(otherProperties);

        String token = otherService.generateToken("alice@example.com", "ADMIN");

        assertThat(jwtService.isTokenValid(token)).isFalse();
        assertThat(jwtService.isAccessTokenValid(token)).isFalse();
    }
}
