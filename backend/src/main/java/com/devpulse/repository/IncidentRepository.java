package com.devpulse.repository;

import com.devpulse.entity.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    Page<Incident> findByProject_IdOrderByCreatedAtDescIdDesc(Long projectId, Pageable pageable);
}
