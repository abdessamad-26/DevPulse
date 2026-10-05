package com.devpulse.dto;

import com.devpulse.entity.Project;

import java.time.LocalDateTime;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        String repository,
        String environment,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(project.getId(), project.getName(), project.getDescription(),
                project.getRepository(), project.getEnvironment(), project.getStatus(),
                project.getCreatedAt(), project.getUpdatedAt());
    }
}
