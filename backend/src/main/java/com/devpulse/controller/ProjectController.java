package com.devpulse.controller;

import com.devpulse.dto.ProjectRequest;
import com.devpulse.entity.Project;
import com.devpulse.service.ProjectAccessService;
import com.devpulse.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Project management endpoints, scoped to the authenticated user.
 * Per the RBAC model: ADMIN and DEVELOPER can create projects, VIEWER can only list them.
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectAccessService projectAccessService;

    public ProjectController(ProjectService projectService, ProjectAccessService projectAccessService) {
        this.projectService = projectService;
        this.projectAccessService = projectAccessService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    public ResponseEntity<Project> create(@Valid @RequestBody ProjectRequest request, Authentication authentication) {
        Long userId = projectAccessService.resolveCurrentUser(authentication).getId();
        Project project = projectService.createProject(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(project);
    }

    @GetMapping
    public ResponseEntity<List<Project>> listMine(Authentication authentication) {
        Long userId = projectAccessService.resolveCurrentUser(authentication).getId();
        return ResponseEntity.ok(projectService.listProjectsAccessibleToUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> get(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(projectAccessService.requireAccessibleProject(id, authentication));
    }
}
