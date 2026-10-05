package com.devpulse.repository;

import com.devpulse.entity.Deployment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, Long> {
    Page<Deployment> findByProject_IdOrderByCreatedAtDescIdDesc(Long projectId, Pageable pageable);

    @Query("""
            SELECT deployment
            FROM Deployment deployment
            WHERE deployment.project.id = :projectId
              AND COALESCE(deployment.finishedAt, deployment.startedAt, deployment.createdAt)
                    BETWEEN :windowStart AND :incidentTime
            ORDER BY COALESCE(deployment.finishedAt, deployment.startedAt, deployment.createdAt) DESC,
                     deployment.id DESC
            """)
    List<Deployment> findCorrelatedDeployments(
            @Param("projectId") Long projectId,
            @Param("windowStart") LocalDateTime windowStart,
            @Param("incidentTime") LocalDateTime incidentTime,
            Pageable pageable);
}
