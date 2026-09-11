package com.devpulse.controller;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class HealthControllerTest {

    @Test
    void healthAndReadyEndpointsReturnExpectedStatus() {
        HealthController controller = new HealthController();
        assertThat(controller.health()).containsEntry("status", "UP");
        assertThat(controller.ready()).containsEntry("status", "READY");
    }
}
