package com.devpulse.controller;

import com.devpulse.dto.AuthRequest;
import com.devpulse.dto.AuthResponse;
import com.devpulse.dto.RefreshRequest;
import com.devpulse.dto.RegisterRequest;
import com.devpulse.dto.UserSummary;
import com.devpulse.entity.User;
import com.devpulse.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
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
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody(required = false) RefreshRequest request) {
        authService.logout(request == null ? null : request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }
}
