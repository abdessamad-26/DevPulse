package com.devpulse.repository;

import com.devpulse.entity.Deployment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, Long> {
    Page<Deployment> findByProject_IdOrderByCreatedAtDescIdDesc(Long projectId, Pageable pageable);
}
