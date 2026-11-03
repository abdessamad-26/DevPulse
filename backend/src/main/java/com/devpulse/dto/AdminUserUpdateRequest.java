package com.devpulse.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AdminUserUpdateRequest(
        @NotNull(message = "Role is required")
        @Pattern(regexp = "ADMIN|DEVELOPER|VIEWER", message = "Role must be ADMIN, DEVELOPER, or VIEWER")
        String role,
        @NotNull(message = "Enabled status is required")
        Boolean enabled) {
}
