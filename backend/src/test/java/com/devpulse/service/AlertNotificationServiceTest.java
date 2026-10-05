package com.devpulse.service;

import com.devpulse.entity.Alert;
import com.devpulse.entity.AlertRule;
import com.devpulse.entity.Project;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertNotificationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldPostOpenedAndResolvedEventsToConfiguredWebhook() throws Exception {
        List<String> requestBodies = new ArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/alerts", exchange -> {
            requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();

        try {
            String webhookUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/alerts";
            AlertNotificationService service = new AlertNotificationService(
                    RestClient.builder(), webhookUrl, Runnable::run);
            Alert alert = alert("OPEN");
            LocalDateTime openedAt = LocalDateTime.of(2026, 10, 5, 12, 5);
            LocalDateTime resolvedAt = LocalDateTime.of(2026, 10, 5, 12, 10);

            service.notifyOpened(alert, 94.0, openedAt);
            alert.setStatus("RESOLVED");
            service.notifyResolved(alert, 42.0, resolvedAt);

            assertThat(requestBodies).hasSize(2);
            JsonNode opened = objectMapper.readTree(requestBodies.get(0));
            assertThat(opened.path("event").asText()).isEqualTo("alert.opened");
            assertThat(opened.path("alertId").asLong()).isEqualTo(7L);
            assertThat(opened.path("projectId").asLong()).isEqualTo(3L);
            assertThat(opened.path("ruleName").asText()).isEqualTo("High CPU");
            assertThat(opened.path("currentValue").asDouble()).isEqualTo(94.0);
            assertThat(opened.path("status").asText()).isEqualTo("OPEN");
            assertThat(opened.path("occurredAt").asText()).isEqualTo(openedAt.toString());

            JsonNode resolved = objectMapper.readTree(requestBodies.get(1));
            assertThat(resolved.path("event").asText()).isEqualTo("alert.resolved");
            assertThat(resolved.path("currentValue").asDouble()).isEqualTo(42.0);
            assertThat(resolved.path("status").asText()).isEqualTo("RESOLVED");
            assertThat(resolved.path("occurredAt").asText()).isEqualTo(resolvedAt.toString());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldSkipDeliveryWhenWebhookIsNotConfigured() {
        AlertNotificationService service = new AlertNotificationService(
                RestClient.builder(), "", Runnable::run);

        service.notifyOpened(alert("OPEN"), 94.0, LocalDateTime.now());
    }

    @Test
    void shouldRejectWebhookUrlsOutsideHttpSchemes() {
        assertThatThrownBy(() -> new AlertNotificationService(
                RestClient.builder(), "file:///tmp/webhook", Runnable::run))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTP(S)");
    }

    @Test
    void shouldNotFailMetricProcessingWhenWebhookReturnsAnError() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/alerts", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();

        try {
            String webhookUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/alerts";
            AlertNotificationService service = new AlertNotificationService(
                    RestClient.builder(), webhookUrl, Runnable::run);

            assertThatCode(() -> service.notifyOpened(alert("OPEN"), 94.0, LocalDateTime.now()))
                    .doesNotThrowAnyException();
        } finally {
            server.stop(0);
        }
    }

    private Alert alert(String status) {
        Project project = new Project();
        project.setId(3L);
        AlertRule rule = new AlertRule();
        rule.setId(11L);
        rule.setName("High CPU");
        rule.setMetric("cpu_usage_percent");
        rule.setOperator(">");
        rule.setThreshold(80.0);
        rule.setProject(project);

        Alert alert = new Alert();
        alert.setId(7L);
        alert.setAlertRule(rule);
        alert.setSeverity("HIGH");
        alert.setStatus(status);
        alert.setMessage("High CPU threshold breached");
        alert.setCreatedAt(LocalDateTime.of(2026, 10, 5, 12, 0));
        return alert;
    }
}
