package com.devpulse.repository;

import com.devpulse.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    Page<Alert> findByAlertRule_Project_IdOrderByCreatedAtDescIdDesc(Long projectId, Pageable pageable);
    Optional<Alert> findFirstByAlertRuleIdAndStatus(Long alertRuleId, String status);
}
