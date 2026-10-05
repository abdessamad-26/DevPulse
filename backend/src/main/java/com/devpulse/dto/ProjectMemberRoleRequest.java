package com.devpulse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ProjectMemberRoleRequest(
        @NotBlank @Pattern(regexp = "DEVELOPER|VIEWER") String role
) {
}
