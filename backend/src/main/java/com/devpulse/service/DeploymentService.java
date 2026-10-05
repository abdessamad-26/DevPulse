package com.devpulse.service;

import com.devpulse.dto.DeploymentRequest;
import com.devpulse.entity.Deployment;
import com.devpulse.entity.Project;
import com.devpulse.exception.ApiException;
import com.devpulse.repository.DeploymentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class DeploymentService {

    private static final Set<String> VALID_STATUSES = Set.of("SUCCESS", "FAILED", "IN_PROGRESS");

    private final DeploymentRepository deploymentRepository;
    private final ProjectAccessService projectAccessService;

    public DeploymentService(DeploymentRepository deploymentRepository, ProjectAccessService projectAccessService) {
        this.deploymentRepository = deploymentRepository;
        this.projectAccessService = projectAccessService;
    }

    public Deployment recordDeployment(Authentication authentication, DeploymentRequest request) {
        Project project = projectAccessService.requireAccessibleProject(request.getProjectId(), authentication);

        String status = request.getStatus().toUpperCase();
        if (!VALID_STATUSES.contains(status)) {
            throw new ApiException("status must be one of " + VALID_STATUSES);
        }

        Deployment deployment = new Deployment();
        deployment.setProject(project);
        deployment.setVersion(request.getVersion());
        deployment.setCommitHash(request.getCommit());
        deployment.setBranch(request.getBranch());
        deployment.setEnvironment(request.getEnvironment());
        deployment.setStatus(status);
        deployment.setStartedAt(request.getStartedAt());
        deployment.setFinishedAt(request.getFinishedAt());
        deployment.setAuthor(authentication.getName());

        return deploymentRepository.save(deployment);
    }

    public List<Deployment> listDeployments(Long projectId, Authentication authentication) {
        projectAccessService.requireAccessibleProject(projectId, authentication);
        return deploymentRepository.findByProject_IdOrderByCreatedAtDesc(projectId);
    }
}
