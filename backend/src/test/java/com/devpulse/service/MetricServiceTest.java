package com.devpulse.service;

import com.devpulse.dto.MetricIngestRequest;
import com.devpulse.entity.Metric;
import com.devpulse.entity.Project;
import com.devpulse.repository.MetricRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricServiceTest {

    @Mock
    private MetricRepository metricRepository;

    @Mock
    private ProjectAccessService projectAccessService;

    @Mock
    private AlertEvaluationService alertEvaluationService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private MetricService metricService;

    @Test
    void shouldPersistEachPointAndEvaluateAlertsForIt() {
        Project project = new Project();
        project.setId(1L);
        when(projectAccessService.requireAccessibleProject(1L, authentication)).thenReturn(project);
        when(metricRepository.save(any(Metric.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MetricIngestRequest.Point point = new MetricIngestRequest.Point();
        point.setMetricName("cpu_usage_percent");
        point.setValue(87.5);
        MetricIngestRequest request = new MetricIngestRequest();
        request.setPoints(List.of(point));

        List<Metric> saved = metricService.ingest(1L, authentication, request);

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getMetricValue()).isEqualTo(87.5);
        verify(alertEvaluationService).evaluate(saved.get(0));
    }

    @Test
    void shouldQueryByMetricNameAndDefaultTimeRangeWhenNotProvided() {
        when(metricRepository.findByProject_IdAndMetricNameAndCapturedAtBetweenOrderByCapturedAtAsc(
                eq(1L), eq("cpu_usage_percent"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(new Metric()));

        List<Metric> results = metricService.query(1L, authentication, "cpu_usage_percent", null, null);

        assertThat(results).hasSize(1);
    }

    @Test
    void shouldListAllMetricsForProjectWhenNoMetricNameGiven() {
        when(metricRepository.findByProject_IdOrderByCapturedAtDesc(1L)).thenReturn(List.of(new Metric(), new Metric()));

        List<Metric> results = metricService.query(1L, authentication, null, null, null);

        assertThat(results).hasSize(2);
    }
}
