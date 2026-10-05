package com.devpulse.service;

import com.devpulse.dto.MetricIngestRequest;
import com.devpulse.entity.Metric;
import com.devpulse.entity.Project;
import com.devpulse.repository.MetricRepository;
import com.devpulse.util.PageRequestSupport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MetricService {

    private final MetricRepository metricRepository;
    private final ProjectAccessService projectAccessService;
    private final AlertEvaluationService alertEvaluationService;

    public MetricService(MetricRepository metricRepository,
                          ProjectAccessService projectAccessService,
                          AlertEvaluationService alertEvaluationService) {
        this.metricRepository = metricRepository;
        this.projectAccessService = projectAccessService;
        this.alertEvaluationService = alertEvaluationService;
    }

    /**
     * Persists every point in the batch and evaluates alert rules against
     * each one as it is ingested (see {@link AlertEvaluationService}).
     */
    public List<Metric> ingest(Long projectId, Authentication authentication, MetricIngestRequest request) {
        Project project = projectAccessService.requireIngestionProject(projectId, authentication);

        return request.getPoints().stream().map(point -> {
            Metric metric = new Metric();
            metric.setProject(project);
            metric.setServiceName(point.getServiceName());
            metric.setMetricName(point.getMetricName());
            metric.setMetricValue(point.getValue());
            metric.setUnit(point.getUnit());
            if (point.getCapturedAt() != null) {
                metric.setCapturedAt(point.getCapturedAt());
            }
            Metric saved = metricRepository.save(metric);
            alertEvaluationService.evaluate(saved);
            return saved;
        }).toList();
    }

    public Page<Metric> query(Long projectId, Authentication authentication, String metricName,
                               LocalDateTime from, LocalDateTime to, int page, int size) {
        projectAccessService.requireAccessibleProject(projectId, authentication);
        Pageable pageable = PageRequestSupport.of(page, size);

        if (metricName == null) {
            return metricRepository.findByProject_IdOrderByCapturedAtDescIdDesc(projectId, pageable);
        }

        LocalDateTime rangeFrom = from != null ? from : LocalDateTime.now().minusDays(1);
        LocalDateTime rangeTo = to != null ? to : LocalDateTime.now();
        return metricRepository.findByProject_IdAndMetricNameAndCapturedAtBetweenOrderByCapturedAtAscIdAsc(
                projectId, metricName, rangeFrom, rangeTo, pageable);
    }
}
