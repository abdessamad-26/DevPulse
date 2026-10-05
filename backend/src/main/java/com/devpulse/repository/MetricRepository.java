package com.devpulse.repository;

import com.devpulse.entity.Metric;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface MetricRepository extends JpaRepository<Metric, Long> {

    Page<Metric> findByProject_IdAndMetricNameAndCapturedAtBetweenOrderByCapturedAtAscIdAsc(
            Long projectId, String metricName, LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<Metric> findByProject_IdOrderByCapturedAtDescIdDesc(Long projectId, Pageable pageable);
}
