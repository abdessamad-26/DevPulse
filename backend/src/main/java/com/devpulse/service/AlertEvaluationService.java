package com.devpulse.service;

import com.devpulse.entity.Alert;
import com.devpulse.entity.AlertRule;
import com.devpulse.entity.Metric;
import com.devpulse.repository.AlertRepository;
import com.devpulse.repository.AlertRuleRepository;
import com.devpulse.repository.MetricRepository;
import com.devpulse.repository.MetricWindowSummary;
import com.devpulse.util.AlertDuration;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Evaluates enabled {@link AlertRule}s against each newly ingested {@link Metric}
 * and opens an {@link Alert} when the threshold is breached. Rules with a
 * configured duration require every observed sample in that window to breach.
 */
@Service
public class AlertEvaluationService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertRepository alertRepository;
    private final MetricRepository metricRepository;

    public AlertEvaluationService(AlertRuleRepository alertRuleRepository, AlertRepository alertRepository,
                                  MetricRepository metricRepository) {
        this.alertRuleRepository = alertRuleRepository;
        this.alertRepository = alertRepository;
        this.metricRepository = metricRepository;
    }

    public void evaluate(Metric metric) {
        Long projectId = metric.getProject().getId();
        List<AlertRule> rules = alertRuleRepository
                .findByProject_IdAndMetricAndEnabledTrue(projectId, metric.getMetricName());

        for (AlertRule rule : rules) {
            if (isBreaching(rule, metric)) {
                openAlertIfNotAlreadyOpen(rule, metric);
            }
        }
    }

    private boolean isBreaching(AlertRule rule, Metric metric) {
        Duration duration = AlertDuration.parse(rule.getDuration());
        if (duration == null) {
            return breaches(rule, metric.getMetricValue());
        }

        LocalDateTime windowStart = metric.getCapturedAt().minus(duration);
        Long projectId = metric.getProject().getId();
        Optional<Metric> precedingSample = metricRepository
                .findFirstByProject_IdAndMetricNameAndCapturedAtLessThanEqualOrderByCapturedAtDescIdDesc(
                        projectId, metric.getMetricName(), windowStart);
        if (precedingSample.isEmpty() || !breaches(rule, precedingSample.get().getMetricValue())) {
            return false;
        }

        MetricWindowSummary summary = metricRepository.summarizeWindow(
                projectId, metric.getMetricName(), windowStart, metric.getCapturedAt());
        if (summary.getSampleCount() == null || summary.getSampleCount() == 0
                || summary.getFirstCapturedAt() == null) {
            return false;
        }

        return switch (rule.getOperator()) {
            case ">" -> summary.getMinimumValue() > rule.getThreshold();
            case "<" -> summary.getMaximumValue() < rule.getThreshold();
            case ">=" -> summary.getMinimumValue() >= rule.getThreshold();
            case "<=" -> summary.getMaximumValue() <= rule.getThreshold();
            case "==" -> summary.getMinimumValue().doubleValue() == rule.getThreshold()
                    && summary.getMaximumValue().doubleValue() == rule.getThreshold();
            default -> false;
        };
    }

    private boolean breaches(AlertRule rule, Double value) {
        double threshold = rule.getThreshold();
        return switch (rule.getOperator()) {
            case ">" -> value > threshold;
            case "<" -> value < threshold;
            case ">=" -> value >= threshold;
            case "<=" -> value <= threshold;
            case "==" -> value.doubleValue() == threshold;
            default -> false;
        };
    }

    private void openAlertIfNotAlreadyOpen(AlertRule rule, Metric metric) {
        boolean alreadyOpen = alertRepository.findFirstByAlertRuleIdAndStatus(rule.getId(), "OPEN").isPresent();
        if (alreadyOpen) {
            return;
        }

        Alert alert = new Alert();
        alert.setAlertRule(rule);
        alert.setSeverity(rule.getSeverity());
        alert.setStatus("OPEN");
        alert.setMessage(String.format(
                "%s: %s %s %s (current value: %s)",
                rule.getName(), rule.getMetric(), rule.getOperator(), rule.getThreshold(), metric.getMetricValue()));
        alertRepository.save(alert);
    }
}
