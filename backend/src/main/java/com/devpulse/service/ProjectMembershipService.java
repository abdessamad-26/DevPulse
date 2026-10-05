package com.devpulse.service;

import com.devpulse.dto.ProjectMemberRequest;
import com.devpulse.dto.ProjectMemberResponse;
import com.devpulse.dto.ProjectMemberRoleRequest;
import com.devpulse.entity.Project;
import com.devpulse.entity.ProjectMember;
import com.devpulse.entity.User;
import com.devpulse.exception.ApiException;
import com.devpulse.repository.ProjectMemberRepository;
import com.devpulse.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProjectMembershipService {

    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final ProjectAccessService projectAccessService;
    private final AuditLogService auditLogService;

    public ProjectMembershipService(ProjectMemberRepository projectMemberRepository,
                                    UserRepository userRepository,
                                    ProjectAccessService projectAccessService,
                                    AuditLogService auditLogService) {
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
        this.projectAccessService = projectAccessService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> listMembers(Long projectId, Authentication authentication) {
        Project project = projectAccessService.requireAccessibleProject(projectId, authentication);
        List<ProjectMemberResponse> members = new ArrayList<>();
        members.add(toResponse(project.getOwner(), "OWNER", project.getCreatedAt()));
        projectMemberRepository.findByProject_IdOrderByCreatedAtAsc(projectId).stream()
                .map(member -> toResponse(member.getUser(), member.getRole(), member.getCreatedAt()))
                .forEach(members::add);
        return members;
    }

    @Transactional
    public ProjectMemberResponse addMember(Long projectId, Authentication authentication,
                                           ProjectMemberRequest request) {
        Project project = projectAccessService.requireProjectManager(projectId, authentication);
        User user = userRepository.findByEmail(request.email().trim())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        if (project.getOwner().getId().equals(user.getId())) {
            throw new ApiException("The project owner is already a member", HttpStatus.CONFLICT);
        }
        if (projectMemberRepository.findByProject_IdAndUser_Id(projectId, user.getId()).isPresent()) {
            throw new ApiException("User is already a member of this project", HttpStatus.CONFLICT);
        }

        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(user);
        member.setRole(request.role());
        ProjectMember saved = projectMemberRepository.save(member);
        auditLogService.recordProject(projectId, projectAccessService.resolveCurrentUser(authentication),
                "PROJECT_MEMBER_ADDED", "PROJECT_MEMBER", user.getId(),
                "role=" + saved.getRole());
        return toResponse(saved);
    }

    @Transactional
    public ProjectMemberResponse updateMemberRole(Long projectId, Long userId, Authentication authentication,
                                                  ProjectMemberRoleRequest request) {
        projectAccessService.requireProjectManager(projectId, authentication);
        ProjectMember member = findMember(projectId, userId);
        String previousRole = member.getRole();
        member.setRole(request.role());
        ProjectMember saved = projectMemberRepository.save(member);
        auditLogService.recordProject(projectId, projectAccessService.resolveCurrentUser(authentication),
                "PROJECT_MEMBER_ROLE_CHANGED", "PROJECT_MEMBER", userId,
                "previousRole=" + previousRole + ";newRole=" + saved.getRole());
        return toResponse(saved);
    }

    @Transactional
    public void removeMember(Long projectId, Long userId, Authentication authentication) {
        projectAccessService.requireProjectManager(projectId, authentication);
        ProjectMember member = findMember(projectId, userId);
        String role = member.getRole();
        projectMemberRepository.delete(member);
        auditLogService.recordProject(projectId, projectAccessService.resolveCurrentUser(authentication),
                "PROJECT_MEMBER_REMOVED", "PROJECT_MEMBER", userId, "role=" + role);
    }

    private ProjectMember findMember(Long projectId, Long userId) {
        return projectMemberRepository.findByProject_IdAndUser_Id(projectId, userId)
                .orElseThrow(() -> new ApiException("Project member not found", HttpStatus.NOT_FOUND));
    }

    private ProjectMemberResponse toResponse(ProjectMember member) {
        return toResponse(member.getUser(), member.getRole(), member.getCreatedAt());
    }

    private ProjectMemberResponse toResponse(User user, String role, java.time.LocalDateTime joinedAt) {
        return new ProjectMemberResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                role, joinedAt);
    }
}
