package com.devpulse.util;

import com.devpulse.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertDurationTest {

    @Test
    void shouldParseCompactAndIsoDurations() {
        assertThat(AlertDuration.parse("5m")).isEqualTo(Duration.ofMinutes(5));
        assertThat(AlertDuration.parse("PT5M")).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void shouldTreatMissingDurationAsImmediateEvaluation() {
        assertThat(AlertDuration.parse(null)).isNull();
        assertThat(AlertDuration.parse("  ")).isNull();
    }

    @Test
    void shouldRejectZeroNegativeAndMalformedDurations() {
        assertThatThrownBy(() -> AlertDuration.parse("0m")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> AlertDuration.parse("PT-5M")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> AlertDuration.parse("five minutes")).isInstanceOf(ApiException.class);
    }
}
