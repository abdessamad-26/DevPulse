package com.devpulse.service;

import com.devpulse.dto.ProjectRequest;
import com.devpulse.entity.Project;
import com.devpulse.entity.User;
import com.devpulse.exception.ApiException;
import com.devpulse.repository.ProjectRepository;
import com.devpulse.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    public Project createProject(Long userId, ProjectRequest request) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));

        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setRepository(request.getRepository());
        project.setEnvironment(request.getEnvironment());
        project.setOwner(owner);
        return projectRepository.save(project);
    }

    /**
     * Named to match {@code ProjectController#listMine}. If you're looking for
     * "findProjectsByUserId", that name was used in a previous revision of this
     * file - keep this method name in sync with the controller and tests.
     */
    public List<Project> listProjectsForOwner(Long userId) {
        return projectRepository.findByOwnerId(userId);
    }
}
