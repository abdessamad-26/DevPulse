package com.devpulse.repository;

import com.devpulse.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByAlertRule_Project_IdOrderByCreatedAtDesc(Long projectId);
    Optional<Alert> findFirstByAlertRuleIdAndStatus(Long alertRuleId, String status);
}
