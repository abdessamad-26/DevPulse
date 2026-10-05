package com.devpulse.dto;

import com.devpulse.entity.Deployment;

import java.time.LocalDateTime;

public record DeploymentResponse(
        Long id,
        Long projectId,
        String version,
        String commitHash,
        String branch,
        String environment,
        String status,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String author,
        LocalDateTime createdAt) {

    public static DeploymentResponse from(Deployment deployment) {
        return new DeploymentResponse(deployment.getId(), deployment.getProjectId(),
                deployment.getVersion(), deployment.getCommitHash(), deployment.getBranch(),
                deployment.getEnvironment(), deployment.getStatus(), deployment.getStartedAt(),
                deployment.getFinishedAt(), deployment.getAuthor(), deployment.getCreatedAt());
    }
}
