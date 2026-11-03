package com.devpulse.util;

import com.devpulse.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;

public final class PasswordRules {

    private static final int BCRYPT_MAX_BYTES = 72;

    private PasswordRules() {}

    public static void requireBcryptCompatible(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new ApiException("Password must not exceed 72 UTF-8 bytes", HttpStatus.BAD_REQUEST);
        }
    }
}
