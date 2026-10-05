package com.devpulse.service;

import com.devpulse.dto.ProjectRequest;
import com.devpulse.entity.Project;
import com.devpulse.entity.User;
import com.devpulse.exception.ApiException;
import com.devpulse.repository.ProjectRepository;
import com.devpulse.repository.UserRepository;
import com.devpulse.util.PageRequestSupport;
import org.springframework.data.domain.Page;
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

    public Page<Project> listProjectsAccessibleToUser(Long userId, int page, int size) {
        return projectRepository.findAccessibleByUserId(userId, PageRequestSupport.of(page, size));
    }
}
