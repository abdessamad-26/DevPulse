package com.devpulse.dto;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String role,
        boolean enabled,
        LocalDateTime createdAt) {
}
