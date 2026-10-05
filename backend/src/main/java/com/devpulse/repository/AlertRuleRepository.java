package com.devpulse.repository;

import com.devpulse.entity.AlertRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {
    Page<AlertRule> findByProject_IdOrderByCreatedAtDescIdDesc(Long projectId, Pageable pageable);
    List<AlertRule> findByProject_IdAndMetricAndEnabledTrue(Long projectId, String metric);
}
