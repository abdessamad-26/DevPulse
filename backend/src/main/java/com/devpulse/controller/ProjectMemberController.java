package com.devpulse.controller;

import com.devpulse.dto.ProjectMemberRequest;
import com.devpulse.dto.ProjectMemberResponse;
import com.devpulse.dto.ProjectMemberRoleRequest;
import com.devpulse.service.ProjectMembershipService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/members")
public class ProjectMemberController {

    private final ProjectMembershipService projectMembershipService;

    public ProjectMemberController(ProjectMembershipService projectMembershipService) {
        this.projectMembershipService = projectMembershipService;
    }

    @GetMapping
    public ResponseEntity<List<ProjectMemberResponse>> list(@PathVariable Long projectId,
                                                              Authentication authentication) {
        return ResponseEntity.ok(projectMembershipService.listMembers(projectId, authentication));
    }

    @PostMapping
    public ResponseEntity<ProjectMemberResponse> add(@PathVariable Long projectId,
                                                       @Valid @RequestBody ProjectMemberRequest request,
                                                       Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectMembershipService.addMember(projectId, authentication, request));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ProjectMemberResponse> updateRole(@PathVariable Long projectId,
                                                              @PathVariable Long userId,
                                                              @Valid @RequestBody ProjectMemberRoleRequest request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(projectMembershipService.updateMemberRole(
                projectId, userId, authentication, request));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> remove(@PathVariable Long projectId, @PathVariable Long userId,
                                       Authentication authentication) {
        projectMembershipService.removeMember(projectId, userId, authentication);
        return ResponseEntity.noContent().build();
    }
}
