package com.devpulse.controller;

import com.devpulse.config.JwtProperties;
import com.devpulse.dto.AuthRequest;
import com.devpulse.dto.AuthResponse;
import com.devpulse.dto.RegisterRequest;
import com.devpulse.dto.UserSummary;
import com.devpulse.entity.User;
import com.devpulse.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "devpulse_refresh";
    private final AuthService authService;
    private final JwtProperties jwtProperties;
    private final boolean secureRefreshCookie;
    private final String refreshCookieSameSite;

    public AuthController(
            AuthService authService,
            JwtProperties jwtProperties,
            @Value("${devpulse.auth.refresh-cookie.secure:false}") boolean secureRefreshCookie,
            @Value("${devpulse.auth.refresh-cookie.same-site:Lax}") String refreshCookieSameSite) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
        this.secureRefreshCookie = secureRefreshCookie;
        this.refreshCookieSameSite = refreshCookieSameSite;
    }

    @PostMapping("/register")
    public ResponseEntity<UserSummary> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        UserSummary summary = new UserSummary(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole() != null ? user.getRole().getName() : null);
        return ResponseEntity.status(HttpStatus.CREATED).body(summary);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return authResponse(authService.authenticate(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {
        return authResponse(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString())
                .build();
    }

    private ResponseEntity<AuthResponse> authResponse(AuthResponse response) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        refreshCookie(response.getRefreshToken(),
                                Duration.ofSeconds(jwtProperties.getRefreshTokenTtlSeconds())).toString())
                .body(response);
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, value)
                .httpOnly(true)
                .secure(secureRefreshCookie)
                .path("/api/auth")
                .sameSite(refreshCookieSameSite)
                .maxAge(maxAge)
                .build();
    }
}
