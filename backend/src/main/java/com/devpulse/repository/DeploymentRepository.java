package com.devpulse.repository;

import com.devpulse.entity.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, Long> {
    List<Deployment> findByProject_IdOrderByCreatedAtDesc(Long projectId);
}
