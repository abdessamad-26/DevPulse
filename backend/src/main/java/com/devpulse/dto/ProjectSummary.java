package com.devpulse.dto;

import com.devpulse.entity.Project;

public record ProjectSummary(Long id, String name, String description, String repository, String environment, String status) {
    public static ProjectSummary from(Project project) {
        return new ProjectSummary(project.getId(), project.getName(), project.getDescription(), project.getRepository(), project.getEnvironment(), project.getStatus());
    }
}