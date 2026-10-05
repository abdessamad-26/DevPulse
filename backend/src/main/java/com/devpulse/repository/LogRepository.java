package com.devpulse.repository;

import com.devpulse.entity.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Uses {@link JpaSpecificationExecutor} so {@code LogService} can build a
 * dynamic filter (service, environment, level, date range, keyword) without
 * a combinatorial explosion of derived query methods.
 */
@Repository
public interface LogRepository extends JpaRepository<LogEntry, Long>, JpaSpecificationExecutor<LogEntry> {
}
