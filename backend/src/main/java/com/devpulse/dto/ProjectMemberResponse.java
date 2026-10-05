package com.devpulse.dto;

import java.time.LocalDateTime;

public record ProjectMemberResponse(
        Long userId,
        String email,
        String firstName,
        String lastName,
        String role,
        LocalDateTime joinedAt
) {
}
