package com.devpulse.repository;

import java.time.LocalDateTime;

public interface MetricWindowSummary {
    Long getSampleCount();
    LocalDateTime getFirstCapturedAt();
    Double getMinimumValue();
    Double getMaximumValue();
}
