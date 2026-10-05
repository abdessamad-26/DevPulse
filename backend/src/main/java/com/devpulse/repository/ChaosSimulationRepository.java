package com.devpulse.repository;

import com.devpulse.entity.ChaosSimulation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChaosSimulationRepository extends JpaRepository<ChaosSimulation, Long> {
    List<ChaosSimulation> findByProject_IdOrderByCreatedAtDesc(Long projectId);
}
