package com.devpulse.service;

import com.devpulse.dto.AuthRequest;
import com.devpulse.dto.AuthResponse;
import com.devpulse.dto.RegisterRequest;
import com.devpulse.dto.UserSummary;
import com.devpulse.entity.Role;
import com.devpulse.entity.RefreshToken;
import com.devpulse.entity.User;
import com.devpulse.exception.ApiException;
import com.devpulse.repository.RefreshTokenRepository;
import com.devpulse.repository.RoleRepository;
import com.devpulse.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                        RoleRepository roleRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        AuditLogService auditLogService,
                        PasswordEncoder passwordEncoder,
                        AuthenticationManager authenticationManager,
                        JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException("Email already exists");
        }

        Role role = roleRepository.findByName("DEVELOPER")
                .orElseThrow(() -> new ApiException("Default role DEVELOPER is not configured"));

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(role);

        return userRepository.save(user);
    }

    @Transactional
    public AuthResponse authenticate(AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException ex) {
            throw new ApiException("Invalid credentials");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException("Invalid credentials"));

        String refreshToken = issueRefreshToken(user, UUID.randomUUID());
        auditLogService.recordGlobal(user, "AUTH_LOGIN", "SESSION", null, null);
        return buildAuthResponse(user, refreshToken);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse refresh(String refreshToken) {
        if (!jwtService.isRefreshTokenValid(refreshToken)) {
            throw new ApiException("Invalid or expired refresh token");
        }

        String tokenHash = hashToken(refreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ApiException("Invalid or expired refresh token"));

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (storedToken.getRevokedAt() != null) {
            refreshTokenRepository.revokeActiveFamily(storedToken.getFamilyId(), now);
            auditLogService.recordGlobal(storedToken.getUser(), "AUTH_REFRESH_REUSE", "SESSION",
                    null, "sessionFamilyRevoked=true");
            throw new ApiException("Refresh token reuse detected; session revoked");
        }
        if (!storedToken.getExpiresAt().isAfter(now)) {
            throw new ApiException("Invalid or expired refresh token");
        }

        String email = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found"));
        if (!user.isEnabled() || !user.getId().equals(storedToken.getUser().getId())) {
            throw new ApiException("Invalid or expired refresh token");
        }

        if (refreshTokenRepository.revokeIfActive(tokenHash, now) != 1) {
            refreshTokenRepository.revokeActiveFamily(storedToken.getFamilyId(), now);
            auditLogService.recordGlobal(storedToken.getUser(), "AUTH_REFRESH_REUSE", "SESSION",
                    null, "sessionFamilyRevoked=true");
            throw new ApiException("Refresh token reuse detected; session revoked");
        }

        String rotatedToken = issueRefreshToken(user, storedToken.getFamilyId());
        auditLogService.recordGlobal(user, "AUTH_REFRESH", "SESSION", null, null);
        return buildAuthResponse(user, rotatedToken);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank() || !jwtService.isRefreshTokenValid(refreshToken)) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hashToken(refreshToken))
                .ifPresent(storedToken -> {
                    refreshTokenRepository.revokeActiveFamily(
                            storedToken.getFamilyId(), LocalDateTime.now(ZoneOffset.UTC));
                    auditLogService.recordGlobal(storedToken.getUser(), "AUTH_LOGOUT", "SESSION", null, null);
                });
    }

    private String issueRefreshToken(User user, UUID familyId) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        refreshTokenRepository.deleteExpiredBefore(now);
        String rawToken = jwtService.generateRefreshToken(user.getEmail());
        RefreshToken storedToken = new RefreshToken();
        storedToken.setUser(user);
        storedToken.setFamilyId(familyId);
        storedToken.setTokenHash(hashToken(rawToken));
        storedToken.setExpiresAt(LocalDateTime.ofInstant(
                jwtService.extractExpiration(rawToken).toInstant(), ZoneOffset.UTC));
        refreshTokenRepository.save(storedToken);
        return rawToken;
    }

    private String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private AuthResponse buildAuthResponse(User user, String refreshToken) {
        String roleName = user.getRole() != null ? user.getRole().getName() : null;
        String accessToken = jwtService.generateToken(user.getEmail(), roleName);
        UserSummary summary = new UserSummary(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), roleName);
        return new AuthResponse(accessToken, refreshToken, summary);
    }
}
