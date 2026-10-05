package com.devpulse.service;

import com.devpulse.dto.DeploymentRequest;
import com.devpulse.entity.Deployment;
import com.devpulse.entity.Project;
import com.devpulse.exception.ApiException;
import com.devpulse.repository.DeploymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeploymentServiceTest {

    @Mock
    private DeploymentRepository deploymentRepository;

    @Mock
    private ProjectAccessService projectAccessService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private DeploymentService deploymentService;

    @Test
    void shouldRecordDeploymentWithAuthenticatedUserAsAuthor() {
        Project project = new Project();
        project.setId(1L);

        when(projectAccessService.requireAccessibleProject(1L, authentication)).thenReturn(project);
        when(authentication.getName()).thenReturn("alice@example.com");
        when(deploymentRepository.save(any(Deployment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeploymentRequest request = new DeploymentRequest();
        request.setProjectId(1L);
        request.setVersion("v1.4.2");
        request.setEnvironment("production");
        request.setStatus("success");

        Deployment deployment = deploymentService.recordDeployment(authentication, request);

        assertThat(deployment.getStatus()).isEqualTo("SUCCESS");
        assertThat(deployment.getAuthor()).isEqualTo("alice@example.com");
        assertThat(deployment.getVersion()).isEqualTo("v1.4.2");
    }

    @Test
    void shouldRejectInvalidDeploymentStatus() {
        Project project = new Project();
        project.setId(1L);
        when(projectAccessService.requireAccessibleProject(1L, authentication)).thenReturn(project);

        DeploymentRequest request = new DeploymentRequest();
        request.setProjectId(1L);
        request.setVersion("v1.4.2");
        request.setEnvironment("production");
        request.setStatus("PENDING");

        assertThatThrownBy(() -> deploymentService.recordDeployment(authentication, request))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void shouldRejectDeploymentOnInaccessibleProject() {
        when(projectAccessService.requireAccessibleProject(1L, authentication))
                .thenThrow(new AccessDeniedException("no access"));

        DeploymentRequest request = new DeploymentRequest();
        request.setProjectId(1L);
        request.setVersion("v1.4.2");
        request.setEnvironment("production");
        request.setStatus("SUCCESS");

        assertThatThrownBy(() -> deploymentService.recordDeployment(authentication, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldListDeploymentsForProject() {
        Deployment deployment = new Deployment();
        deployment.setVersion("v1.4.2");

        when(deploymentRepository.findByProject_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of(deployment));

        List<Deployment> deployments = deploymentService.listDeployments(1L, authentication);

        assertThat(deployments).hasSize(1);
        assertThat(deployments.get(0).getVersion()).isEqualTo("v1.4.2");
    }
}
