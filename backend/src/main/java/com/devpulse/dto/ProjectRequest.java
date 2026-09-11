package com.devpulse.dto;

import jakarta.validation.constraints.NotBlank;
public class ProjectRequest {

    @NotBlank(message = "Project name is required")
    private String name;

    private String description;

    private String repository;

    @NotBlank(message = "Environment is required")
    private String environment;

    public ProjectRequest() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }
}
