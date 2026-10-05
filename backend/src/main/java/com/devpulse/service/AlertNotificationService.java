package com.devpulse.service;

import com.devpulse.dto.AlertWebhookPayload;
import com.devpulse.entity.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AlertNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AlertNotificationService.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(3);

    private final String webhookUrl;
    private final Optional<RestClient> restClient;
    private final TaskExecutor executor;

    public AlertNotificationService(
            RestClient.Builder restClientBuilder,
            @Value("${devpulse.alerts.webhook-url:}") String webhookUrl,
            @Qualifier("alertWebhookExecutor") TaskExecutor executor) {
        this.webhookUrl = webhookUrl == null ? "" : webhookUrl.trim();
        this.executor = executor;
        this.restClient = this.webhookUrl.isBlank()
                ? Optional.empty()
                : Optional.of(createRestClient(restClientBuilder, this.webhookUrl));

        if (this.webhookUrl.isBlank()) {
            log.info("Alert webhook notifications are disabled; set ALERT_WEBHOOK_URL to enable delivery");
        }
    }

    public void notifyOpened(Alert alert, Double currentValue, LocalDateTime occurredAt) {
        enqueue("alert.opened", alert, currentValue, occurredAt);
    }

    public void notifyResolved(Alert alert, Double currentValue, LocalDateTime occurredAt) {
        enqueue("alert.resolved", alert, currentValue, occurredAt);
    }

    private void enqueue(String event, Alert alert, Double currentValue, LocalDateTime occurredAt) {
        if (restClient.isEmpty()) {
            return;
        }

        AlertWebhookPayload payload = new AlertWebhookPayload(
                event,
                alert.getId(),
                alert.getAlertRule().getProjectId(),
                alert.getAlertRule().getId(),
                alert.getAlertRule().getName(),
                alert.getAlertRule().getMetric(),
                alert.getAlertRule().getOperator(),
                alert.getAlertRule().getThreshold(),
                currentValue,
                alert.getSeverity(),
                alert.getStatus(),
                alert.getMessage(),
                occurredAt.toString());

        try {
            executor.execute(() -> deliver(payload));
        } catch (TaskRejectedException exception) {
            log.warn("Alert webhook queue is full; dropped {} notification for alert {}", event, alert.getId());
        }
    }

    private void deliver(AlertWebhookPayload payload) {
        try {
            restClient.orElseThrow().post()
                    .uri(webhookUrl)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            log.warn("Alert webhook delivery failed for alert {} ({}): {}",
                    payload.alertId(), payload.event(), exception.getClass().getSimpleName());
        }
    }

    private RestClient createRestClient(RestClient.Builder builder, String configuredUrl) {
        URI uri;
        try {
            uri = URI.create(configuredUrl);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("ALERT_WEBHOOK_URL must be a valid HTTP(S) URL", exception);
        }

        String scheme = uri.getScheme();
        if (!uri.isAbsolute() || uri.getHost() == null
                || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                || uri.getUserInfo() != null || uri.getFragment() != null) {
            throw new IllegalStateException("ALERT_WEBHOOK_URL must be an absolute HTTP(S) URL without credentials or fragments");
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return builder.requestFactory(requestFactory).build();
    }
}
