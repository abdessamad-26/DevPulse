package com.devpulse.controller;

import com.devpulse.dto.AdminUserResponse;
import com.devpulse.dto.AdminUserUpdateRequest;
import com.devpulse.dto.PageResponse;
import com.devpulse.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<AdminUserResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return ResponseEntity.ok(PageResponse.from(userAdminService.listUsers(authentication, page, size)));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<AdminUserResponse> updateUser(@PathVariable Long userId,
                                                        @Valid @RequestBody AdminUserUpdateRequest request,
                                                        Authentication authentication) {
        return ResponseEntity.ok(userAdminService.updateUser(authentication, userId, request));
    }
}
