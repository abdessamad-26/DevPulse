package com.devpulse.repository;

import com.devpulse.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    @Query(value = """
            SELECT p FROM Project p
            WHERE p.owner.id = :userId
               OR EXISTS (
                   SELECT pm.id FROM ProjectMember pm
                   WHERE pm.project.id = p.id AND pm.user.id = :userId
               )
            ORDER BY p.createdAt DESC, p.id DESC
            """,
            countQuery = """
            SELECT COUNT(p) FROM Project p
            WHERE p.owner.id = :userId
               OR EXISTS (
                   SELECT pm.id FROM ProjectMember pm
                   WHERE pm.project.id = p.id AND pm.user.id = :userId
               )
            """)
    Page<Project> findAccessibleByUserId(@Param("userId") Long userId, Pageable pageable);
}
