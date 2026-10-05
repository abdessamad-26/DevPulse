package com.devpulse.repository;

import com.devpulse.entity.Metric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MetricRepository extends JpaRepository<Metric, Long> {

    List<Metric> findByProject_IdAndMetricNameAndCapturedAtBetweenOrderByCapturedAtAsc(
            Long projectId, String metricName, LocalDateTime from, LocalDateTime to);

    List<Metric> findByProject_IdOrderByCapturedAtDesc(Long projectId);
}
