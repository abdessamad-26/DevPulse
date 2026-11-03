package com.devpulse.service;

import com.devpulse.dto.AdminUserResponse;
import com.devpulse.dto.AdminUserUpdateRequest;
import com.devpulse.entity.Role;
import com.devpulse.entity.User;
import com.devpulse.exception.ApiException;
import com.devpulse.repository.RefreshTokenRepository;
import com.devpulse.repository.RoleRepository;
import com.devpulse.repository.UserRepository;
import com.devpulse.util.PageRequestSupport;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditLogService auditLogService;
    private final ProjectAccessService projectAccessService;

    public UserAdminService(UserRepository userRepository,
                            RoleRepository roleRepository,
                            RefreshTokenRepository refreshTokenRepository,
                            AuditLogService auditLogService,
                            ProjectAccessService projectAccessService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditLogService = auditLogService;
        this.projectAccessService = projectAccessService;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listUsers(Authentication authentication, int page, int size) {
        projectAccessService.requireGlobalAdmin(authentication);
        return userRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequestSupport.of(page, size))
                .map(this::toResponse);
    }

    @Transactional
    public AdminUserResponse updateUser(Authentication authentication, Long userId, AdminUserUpdateRequest request) {
        User actor = projectAccessService.requireGlobalAdmin(authentication);
        if (actor.getId().equals(userId)) {
            throw new ApiException("Administrators cannot change their own role or enabled status", HttpStatus.CONFLICT);
        }

        // Serialize global-role changes so two administrators cannot demote the last active administrator concurrently.
        Role adminRole = roleRepository.findByNameForUpdate("ADMIN")
                .orElseThrow(() -> new ApiException("Role ADMIN is not configured"));
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        Role role = roleRepository.findByName(request.role())
                .orElseThrow(() -> new ApiException("Role " + request.role() + " is not configured"));

        boolean isActiveAdmin = user.isEnabled() && user.getRole() != null
                && "ADMIN".equals(user.getRole().getName());
        boolean removesAdminAccess = isActiveAdmin && (!request.enabled() || !"ADMIN".equals(role.getName()));
        if (removesAdminAccess && userRepository.countByRole_NameAndEnabledTrue(adminRole.getName()) <= 1) {
            throw new ApiException("The last active administrator cannot be disabled or demoted", HttpStatus.CONFLICT);
        }

        String previousRole = user.getRole() == null ? null : user.getRole().getName();
        boolean previousEnabled = user.isEnabled();
        boolean changed = !role.getName().equals(previousRole) || request.enabled() != previousEnabled;
        if (changed) {
            user.setRole(role);
            user.setEnabled(request.enabled());
            user.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            userRepository.save(user);
            refreshTokenRepository.revokeAllActiveByUserId(user.getId(), LocalDateTime.now(ZoneOffset.UTC));
            String details = "role:" + previousRole + "->" + role.getName()
                    + ";enabled:" + previousEnabled + "->" + request.enabled();
            auditLogService.recordGlobal(actor, "ADMIN_USER_UPDATED", "USER", user.getId(), details);
        }
        return toResponse(user);
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getRole() == null ? null : user.getRole().getName(), user.isEnabled(), user.getCreatedAt());
    }
}
