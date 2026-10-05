package com.devpulse.repository;

import com.devpulse.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByProject_IdOrderByCreatedAtDescIdDesc(Long projectId, Pageable pageable);

    Page<AuditLog> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
}
