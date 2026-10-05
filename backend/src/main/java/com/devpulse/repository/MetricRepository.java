package com.devpulse.repository;

import com.devpulse.entity.Metric;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface MetricRepository extends JpaRepository<Metric, Long> {

    Page<Metric> findByProject_IdAndMetricNameAndCapturedAtBetweenOrderByCapturedAtAscIdAsc(
            Long projectId, String metricName, LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<Metric> findByProject_IdOrderByCapturedAtDescIdDesc(Long projectId, Pageable pageable);

    Optional<Metric> findFirstByProject_IdAndMetricNameAndCapturedAtLessThanEqualOrderByCapturedAtDescIdDesc(
            Long projectId, String metricName, LocalDateTime capturedAt);

    @Query("""
            SELECT COUNT(metric) AS sampleCount,
                   MIN(metric.capturedAt) AS firstCapturedAt,
                   MIN(metric.metricValue) AS minimumValue,
                   MAX(metric.metricValue) AS maximumValue
            FROM Metric metric
            WHERE metric.project.id = :projectId
              AND metric.metricName = :metricName
              AND metric.capturedAt > :windowStart
              AND metric.capturedAt <= :windowEnd
            """)
    MetricWindowSummary summarizeWindow(
            @Param("projectId") Long projectId,
            @Param("metricName") String metricName,
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd") LocalDateTime windowEnd);
}
