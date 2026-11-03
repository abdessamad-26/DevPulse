package com.devpulse.service;

import com.devpulse.dto.ChangePasswordRequest;
import com.devpulse.dto.ProfileUpdateRequest;
import com.devpulse.dto.UserSummary;
import com.devpulse.entity.User;
import com.devpulse.exception.ApiException;
import com.devpulse.repository.RefreshTokenRepository;
import com.devpulse.repository.UserRepository;
import com.devpulse.util.PasswordRules;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class UserAccountService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserRepository userRepository,
                              RefreshTokenRepository refreshTokenRepository,
                              AuditLogService auditLogService,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserSummary updateProfile(Authentication authentication, ProfileUpdateRequest request) {
        User user = findCurrentUser(authentication);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        User saved = userRepository.save(user);
        auditLogService.recordGlobal(saved, "ACCOUNT_PROFILE_UPDATED", "USER", saved.getId(), null);
        return toSummary(saved);
    }

    @Transactional
    public void changePassword(Authentication authentication, ChangePasswordRequest request) {
        User user = findCurrentUser(authentication);
        PasswordRules.requireBcryptCompatible(request.newPassword());
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ApiException("Current password is incorrect", HttpStatus.BAD_REQUEST);
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new ApiException("New password must be different from the current password", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        userRepository.save(user);
        refreshTokenRepository.revokeAllActiveByUserId(user.getId(), LocalDateTime.now(ZoneOffset.UTC));
        auditLogService.recordGlobal(user, "ACCOUNT_PASSWORD_CHANGED", "USER", user.getId(), "activeSessionsRevoked=true");
    }

    private User findCurrentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    }

    private UserSummary toSummary(User user) {
        return new UserSummary(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getRole() == null ? null : user.getRole().getName());
    }
}
