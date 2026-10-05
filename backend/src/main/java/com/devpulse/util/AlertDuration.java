package com.devpulse.util;

import com.devpulse.exception.ApiException;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AlertDuration {

    private static final Pattern SIMPLE_DURATION = Pattern.compile("^(\\d+)(ms|s|m|h|d)$");
    private static final String INVALID_DURATION_MESSAGE =
            "duration must be positive, for example 5m or PT5M";

    private AlertDuration() {}

    public static Duration parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        try {
            Duration duration = parseSupportedFormat(normalized);
            if (duration.isZero() || duration.isNegative()) {
                throw new ApiException(INVALID_DURATION_MESSAGE);
            }
            LocalDateTime.now().minus(duration);
            return duration;
        } catch (DateTimeException | ArithmeticException | NumberFormatException exception) {
            throw new ApiException(INVALID_DURATION_MESSAGE);
        }
    }

    private static Duration parseSupportedFormat(String value) {
        Matcher matcher = SIMPLE_DURATION.matcher(value);
        if (!matcher.matches()) {
            return Duration.parse(value.toUpperCase(Locale.ROOT));
        }

        long amount = Long.parseLong(matcher.group(1));
        return switch (matcher.group(2)) {
            case "ms" -> Duration.ofMillis(amount);
            case "s" -> Duration.ofSeconds(amount);
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            case "d" -> Duration.ofDays(amount);
            default -> throw new ApiException(INVALID_DURATION_MESSAGE);
        };
    }
}
