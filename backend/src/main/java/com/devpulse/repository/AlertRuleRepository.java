package com.devpulse.repository;

import com.devpulse.entity.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {
    List<AlertRule> findByProject_Id(Long projectId);
    List<AlertRule> findByProject_IdAndMetricAndEnabledTrue(Long projectId, String metric);
}
