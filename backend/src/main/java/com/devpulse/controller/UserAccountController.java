package com.devpulse.controller;

import com.devpulse.dto.ChangePasswordRequest;
import com.devpulse.dto.ProfileUpdateRequest;
import com.devpulse.dto.UserSummary;
import com.devpulse.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/users/me")
public class UserAccountController {

    private final UserAccountService userAccountService;
    private final boolean secureRefreshCookie;
    private final String refreshCookieSameSite;

    public UserAccountController(UserAccountService userAccountService,
                                 @Value("${devpulse.auth.refresh-cookie.secure:false}") boolean secureRefreshCookie,
                                 @Value("${devpulse.auth.refresh-cookie.same-site:Lax}") String refreshCookieSameSite) {
        this.userAccountService = userAccountService;
        this.secureRefreshCookie = secureRefreshCookie;
        this.refreshCookieSameSite = refreshCookieSameSite;
    }

    @PatchMapping
    public ResponseEntity<UserSummary> updateProfile(@Valid @RequestBody ProfileUpdateRequest request,
                                                     Authentication authentication) {
        return ResponseEntity.ok(userAccountService.updateProfile(authentication, request));
    }

    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                               Authentication authentication) {
        userAccountService.changePassword(authentication, request);
        ResponseCookie expiredCookie = ResponseCookie.from("devpulse_refresh", "")
                .httpOnly(true)
                .secure(secureRefreshCookie)
                .path("/api/auth")
                .sameSite(refreshCookieSameSite)
                .maxAge(Duration.ZERO)
                .build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, expiredCookie.toString()).build();
    }
}
