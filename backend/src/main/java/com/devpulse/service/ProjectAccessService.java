package com.devpulse.service;

import com.devpulse.entity.Project;
import com.devpulse.entity.ProjectMember;
import com.devpulse.entity.User;
import com.devpulse.exception.ApiException;
import com.devpulse.repository.ProjectMemberRepository;
import com.devpulse.repository.ProjectRepository;
import com.devpulse.repository.UserRepository;
import com.devpulse.security.IngestionApiKeyPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Central place for "does this user own / can this user touch this project"
 * checks, used by every controller that manages project-scoped resources
 * (services, incidents, deployments, ...). Keeping it in one place avoids
 * duplicating (and potentially diverging) the same check in every controller.
 */
@Service
public class ProjectAccessService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    public ProjectAccessService(ProjectRepository projectRepository,
                                ProjectMemberRepository projectMemberRepository,
                                UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
    }

    public User resolveCurrentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ApiException("Authenticated user not found"));
    }

    /**
     * Returns the project if the current user is its owner, an ADMIN, or a member,
     * otherwise throws {@link AccessDeniedException} (mapped to HTTP 403 by
     * {@link com.devpulse.exception.GlobalExceptionHandler}).
     */
    public Project requireAccessibleProject(Long projectId, Authentication authentication) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException("Project not found"));
        User user = resolveCurrentUser(authentication);

        if (!isAdmin(user) && !isOwner(project, user) && findMembership(projectId, user) == null) {
            throw new AccessDeniedException("You do not have access to this project");
        }

        return project;
    }

    public Project requireWritableProject(Long projectId, Authentication authentication) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException("Project not found"));
        User user = resolveCurrentUser(authentication);

        if (isAdmin(user) || isOwner(project, user)) {
            return project;
        }
        ProjectMember membership = findMembership(projectId, user);
        if (membership == null || !"DEVELOPER".equals(membership.getRole())) {
            throw new AccessDeniedException("A DEVELOPER project role is required to modify this project");
        }
        return project;
    }

    public Project requireIngestionProject(Long projectId, Authentication authentication) {
        if (authentication.getPrincipal() instanceof IngestionApiKeyPrincipal apiKeyPrincipal) {
            if (!apiKeyPrincipal.projectId().equals(projectId)) {
                throw new AccessDeniedException("This ingestion API key is scoped to a different project");
            }
            return projectRepository.findById(projectId)
                    .orElseThrow(() -> new ApiException("Project not found"));
        }
        return requireWritableProject(projectId, authentication);
    }

    public Project requireProjectManager(Long projectId, Authentication authentication) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException("Project not found"));
        User user = resolveCurrentUser(authentication);

        if (!isAdmin(user) && !isOwner(project, user)) {
            throw new AccessDeniedException("Only the project owner or an ADMIN can manage project members");
        }
        return project;
    }

    public void requireGlobalAdmin(Authentication authentication) {
        User user = resolveCurrentUser(authentication);
        if (!isAdmin(user)) {
            throw new AccessDeniedException("Only a global ADMIN can access all audit logs");
        }
    }

    /**
     * Guard for resources that are not attached to any project (legacy or
     * orphaned rows, since project_id is nullable in the schema). There is no
     * owner to check, so skipping the check would let ANY authenticated user
     * read or modify them; only ADMIN may touch such resources.
     */
    public void requireAdminForUnscopedResource(Authentication authentication) {
        User user = resolveCurrentUser(authentication);
        if (!isAdmin(user)) {
            throw new AccessDeniedException("This resource is not attached to a project; only an ADMIN can access it");
        }
    }

    private boolean isAdmin(User user) {
        return user.getRole() != null && "ADMIN".equals(user.getRole().getName());
    }

    private boolean isOwner(Project project, User user) {
        return project.getOwner() != null && project.getOwner().getId().equals(user.getId());
    }

    private ProjectMember findMembership(Long projectId, User user) {
        return projectMemberRepository.findByProject_IdAndUser_Id(projectId, user.getId()).orElse(null);
    }
}
