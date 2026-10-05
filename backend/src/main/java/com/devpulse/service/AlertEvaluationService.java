package com.devpulse.service;

import com.devpulse.entity.Alert;
import com.devpulse.entity.AlertRule;
import com.devpulse.entity.Metric;
import com.devpulse.repository.AlertRepository;
import com.devpulse.repository.AlertRuleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Evaluates enabled {@link AlertRule}s against each newly ingested {@link Metric}
 * and opens an {@link Alert} when the threshold is breached.
 *
 * Known limitation (see docs/architecture.md#16): the "duration" field on an
 * AlertRule (e.g. "5m") is stored but NOT evaluated - a single breaching data
 * point triggers the alert immediately rather than requiring the condition to
 * hold for a time window. Implementing that properly needs either a
 * time-series query or a scheduled evaluator, which is out of scope for this
 * increment.
 */
@Service
public class AlertEvaluationService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertRepository alertRepository;

    public AlertEvaluationService(AlertRuleRepository alertRuleRepository, AlertRepository alertRepository) {
        this.alertRuleRepository = alertRuleRepository;
        this.alertRepository = alertRepository;
    }

    public void evaluate(Metric metric) {
        Long projectId = metric.getProject().getId();
        List<AlertRule> rules = alertRuleRepository
                .findByProject_IdAndMetricAndEnabledTrue(projectId, metric.getMetricName());

        for (AlertRule rule : rules) {
            if (breaches(rule, metric.getMetricValue())) {
                openAlertIfNotAlreadyOpen(rule, metric);
            }
        }
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
