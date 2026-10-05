package com.devpulse.repository;

import com.devpulse.entity.ChaosSimulation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChaosSimulationRepository extends JpaRepository<ChaosSimulation, Long> {
    Page<ChaosSimulation> findByProject_IdOrderByCreatedAtDescIdDesc(Long projectId, Pageable pageable);
}
