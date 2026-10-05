package com.devpulse.repository;

import com.devpulse.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    Optional<ProjectMember> findByProject_IdAndUser_Id(Long projectId, Long userId);

    List<ProjectMember> findByProject_IdOrderByCreatedAtAsc(Long projectId);
}
