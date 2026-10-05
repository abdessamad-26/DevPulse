package com.devpulse.service;

import com.devpulse.entity.Project;
import com.devpulse.entity.User;
import com.devpulse.exception.ApiException;
import com.devpulse.repository.ProjectRepository;
import com.devpulse.repository.UserRepository;
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
    private final UserRepository userRepository;

    public ProjectAccessService(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    public User resolveCurrentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ApiException("Authenticated user not found"));
    }

    /**
     * Returns the project if the current user is its owner or an ADMIN,
     * otherwise throws {@link AccessDeniedException} (mapped to HTTP 403 by
     * {@link com.devpulse.exception.GlobalExceptionHandler}).
     */
    public Project requireAccessibleProject(Long projectId, Authentication authentication) {
        User user = resolveCurrentUser(authentication);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException("Project not found"));

        boolean isAdmin = isAdmin(user);
        boolean isOwner = project.getOwner() != null && project.getOwner().getId().equals(user.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You do not have access to this project");
        }

        return project;
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
}
