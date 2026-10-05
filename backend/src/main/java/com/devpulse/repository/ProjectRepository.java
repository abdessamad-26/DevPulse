package com.devpulse.repository;

import com.devpulse.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    @Query("""
            SELECT p FROM Project p
            WHERE p.owner.id = :userId
               OR EXISTS (
                   SELECT pm.id FROM ProjectMember pm
                   WHERE pm.project.id = p.id AND pm.user.id = :userId
               )
            ORDER BY p.createdAt DESC
            """)
    List<Project> findAccessibleByUserId(@Param("userId") Long userId);
}
