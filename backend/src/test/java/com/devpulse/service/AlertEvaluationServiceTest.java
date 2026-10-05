package com.devpulse.service;

import com.devpulse.entity.Alert;
import com.devpulse.entity.AlertRule;
import com.devpulse.entity.Metric;
import com.devpulse.entity.Project;
import com.devpulse.repository.AlertRepository;
import com.devpulse.repository.AlertRuleRepository;
import com.devpulse.repository.MetricRepository;
import com.devpulse.repository.MetricWindowSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertEvaluationServiceTest {

    @Mock
    private AlertRuleRepository alertRuleRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private MetricRepository metricRepository;

    @InjectMocks
    private AlertEvaluationService alertEvaluationService;

    private Metric metricAbove(double value) {
        Project project = new Project();
        project.setId(1L);
        Metric metric = new Metric();
        metric.setProject(project);
        metric.setMetricName("cpu_usage_percent");
        metric.setMetricValue(value);
        return metric;
    }

    private AlertRule rule(String operator, double threshold) {
        AlertRule rule = new AlertRule();
        rule.setId(42L);
        rule.setName("High CPU");
        rule.setMetric("cpu_usage_percent");
        rule.setOperator(operator);
        rule.setThreshold(threshold);
        rule.setSeverity("HIGH");
        rule.setEnabled(true);
        return rule;
    }

    @Test
    void shouldOpenAlertWhenThresholdIsBreached() {
        AlertRule rule = rule(">", 80.0);
        when(alertRuleRepository.findByProject_IdAndMetricAndEnabledTrue(1L, "cpu_usage_percent"))
                .thenReturn(List.of(rule));
        when(alertRepository.findFirstByAlertRuleIdAndStatus(42L, "OPEN")).thenReturn(Optional.empty());

        alertEvaluationService.evaluate(metricAbove(92.0));

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo("HIGH");
        assertThat(captor.getValue().getStatus()).isEqualTo("OPEN");
        assertThat(captor.getValue().getMessage()).contains("92.0");
    }

    @Test
    void shouldNotOpenAlertWhenValueIsUnderThreshold() {
        AlertRule rule = rule(">", 80.0);
        when(alertRuleRepository.findByProject_IdAndMetricAndEnabledTrue(1L, "cpu_usage_percent"))
                .thenReturn(List.of(rule));

        alertEvaluationService.evaluate(metricAbove(45.0));

        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void shouldNotDuplicateAlertWhenOneIsAlreadyOpenForTheRule() {
        AlertRule rule = rule(">", 80.0);
        when(alertRuleRepository.findByProject_IdAndMetricAndEnabledTrue(1L, "cpu_usage_percent"))
                .thenReturn(List.of(rule));
        when(alertRepository.findFirstByAlertRuleIdAndStatus(42L, "OPEN"))
                .thenReturn(Optional.of(new Alert()));

        alertEvaluationService.evaluate(metricAbove(95.0));

        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void shouldSupportLessThanOperator() {
        AlertRule rule = rule("<", 10.0);
        when(alertRuleRepository.findByProject_IdAndMetricAndEnabledTrue(1L, "cpu_usage_percent"))
                .thenReturn(List.of(rule));
        when(alertRepository.findFirstByAlertRuleIdAndStatus(42L, "OPEN")).thenReturn(Optional.empty());

        alertEvaluationService.evaluate(metricAbove(5.0));

        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void shouldWaitForTheConfiguredWindowBeforeOpeningAnAlert() {
        AlertRule rule = rule(">", 80.0);
        rule.setDuration("5m");
        when(alertRuleRepository.findByProject_IdAndMetricAndEnabledTrue(1L, "cpu_usage_percent"))
                .thenReturn(List.of(rule));

        alertEvaluationService.evaluate(metricAbove(92.0));

        verify(metricRepository).findFirstByProject_IdAndMetricNameAndCapturedAtLessThanEqualOrderByCapturedAtDescIdDesc(
                eq(1L), eq("cpu_usage_percent"), any(LocalDateTime.class));
        verify(metricRepository, never()).summarizeWindow(any(), any(), any(), any());
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void shouldOpenAnAlertAfterAllSamplesAcrossTheWindowBreach() {
        LocalDateTime windowStart = LocalDateTime.of(2026, 1, 1, 12, 0);
        Metric current = metricAbove(92.0);
        current.setCapturedAt(windowStart.plusMinutes(5));
        Metric preceding = metricAbove(89.0);
        preceding.setCapturedAt(windowStart);
        AlertRule rule = rule(">", 80.0);
        rule.setDuration("PT5M");
        when(alertRuleRepository.findByProject_IdAndMetricAndEnabledTrue(1L, "cpu_usage_percent"))
                .thenReturn(List.of(rule));
        when(metricRepository
                .findFirstByProject_IdAndMetricNameAndCapturedAtLessThanEqualOrderByCapturedAtDescIdDesc(
                        1L, "cpu_usage_percent", windowStart))
                .thenReturn(Optional.of(preceding));
        MetricWindowSummary summary = mock(MetricWindowSummary.class);
        when(summary.getSampleCount()).thenReturn(1L);
        when(summary.getFirstCapturedAt()).thenReturn(windowStart.plusMinutes(5));
        when(summary.getMinimumValue()).thenReturn(92.0);
        when(metricRepository.summarizeWindow(1L, "cpu_usage_percent", windowStart,
                windowStart.plusMinutes(5))).thenReturn(summary);
        when(alertRepository.findFirstByAlertRuleIdAndStatus(42L, "OPEN")).thenReturn(Optional.empty());

        alertEvaluationService.evaluate(current);

        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void shouldNotOpenAnAlertWhenAnySampleInsideTheWindowIsBelowThreshold() {
        LocalDateTime windowStart = LocalDateTime.of(2026, 1, 1, 12, 0);
        Metric current = metricAbove(92.0);
        current.setCapturedAt(windowStart.plusMinutes(5));
        Metric preceding = metricAbove(89.0);
        preceding.setCapturedAt(windowStart);
        AlertRule rule = rule(">", 80.0);
        rule.setDuration("5m");
        when(alertRuleRepository.findByProject_IdAndMetricAndEnabledTrue(1L, "cpu_usage_percent"))
                .thenReturn(List.of(rule));
        when(metricRepository
                .findFirstByProject_IdAndMetricNameAndCapturedAtLessThanEqualOrderByCapturedAtDescIdDesc(
                        1L, "cpu_usage_percent", windowStart))
                .thenReturn(Optional.of(preceding));
        MetricWindowSummary summary = mock(MetricWindowSummary.class);
        when(summary.getSampleCount()).thenReturn(2L);
        when(summary.getFirstCapturedAt()).thenReturn(windowStart.plusMinutes(2));
        when(summary.getMinimumValue()).thenReturn(70.0);
        when(metricRepository.summarizeWindow(1L, "cpu_usage_percent", windowStart,
                windowStart.plusMinutes(5))).thenReturn(summary);

        alertEvaluationService.evaluate(current);

        verify(alertRepository, never()).save(any(Alert.class));
    }
}
