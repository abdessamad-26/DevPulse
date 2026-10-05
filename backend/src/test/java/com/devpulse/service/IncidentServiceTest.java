package com.devpulse.service;

import com.devpulse.dto.IncidentAnalysisResponse;
import com.devpulse.dto.IncidentCreateRequest;
import com.devpulse.dto.IncidentUpdateRequest;
import com.devpulse.entity.Incident;
import com.devpulse.entity.Deployment;
import com.devpulse.entity.Project;
import com.devpulse.entity.ServiceEntity;
import com.devpulse.exception.ApiException;
import com.devpulse.repository.DeploymentRepository;
import com.devpulse.repository.IncidentRepository;
import com.devpulse.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private DeploymentRepository deploymentRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private ProjectAccessService projectAccessService;

    @Mock
    private AiAnalysisService aiAnalysisService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private IncidentService incidentService;

    @Test
    void shouldCreateIncidentWithValidSeverity() {
        Project project = new Project();
        project.setId(1L);

        when(projectAccessService.requireWritableProject(1L, authentication)).thenReturn(project);
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IncidentCreateRequest request = new IncidentCreateRequest();
        request.setProjectId(1L);
        request.setTitle("Order API latency spike");
        request.setSeverity("high");

        Incident incident = incidentService.createIncident(authentication, request);

        assertThat(incident.getSeverity()).isEqualTo("HIGH");
        assertThat(incident.getStatus()).isEqualTo("OPEN");
        assertThat(incident.getStartedAt()).isNotNull();
    }

    @Test
    void shouldRejectInvalidSeverity() {
        Project project = new Project();
        project.setId(1L);
        when(projectAccessService.requireWritableProject(1L, authentication)).thenReturn(project);

        IncidentCreateRequest request = new IncidentCreateRequest();
        request.setProjectId(1L);
        request.setTitle("Something broke");
        request.setSeverity("URGENT");

        assertThatThrownBy(() -> incidentService.createIncident(authentication, request))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void shouldRejectIncidentCreationOnInaccessibleProject() {
        when(projectAccessService.requireWritableProject(1L, authentication))
                .thenThrow(new AccessDeniedException("no access"));

        IncidentCreateRequest request = new IncidentCreateRequest();
        request.setProjectId(1L);
        request.setTitle("Something broke");
        request.setSeverity("HIGH");

        assertThatThrownBy(() -> incidentService.createIncident(authentication, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldLinkKnownServiceWhenServiceIdProvided() {
        Project project = new Project();
        project.setId(1L);
        ServiceEntity service = new ServiceEntity();
        service.setId(5L);
        service.setName("billing-api");
        service.setProject(project);

        when(projectAccessService.requireWritableProject(1L, authentication)).thenReturn(project);
        when(serviceRepository.findById(5L)).thenReturn(Optional.of(service));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IncidentCreateRequest request = new IncidentCreateRequest();
        request.setProjectId(1L);
        request.setServiceId(5L);
        request.setTitle("Billing errors");
        request.setSeverity("CRITICAL");

        Incident incident = incidentService.createIncident(authentication, request);

        assertThat(incident.getServiceRef()).isEqualTo(service);
        assertThat(incident.getServiceName()).isEqualTo("billing-api");
    }

    @Test
    void shouldSetResolvedAtWhenStatusMovesToResolved() {
        Project project = new Project();
        project.setId(1L);
        Incident incident = new Incident();
        incident.setId(10L);
        incident.setProject(project);
        incident.setStatus("INVESTIGATING");

        when(incidentRepository.findById(10L)).thenReturn(Optional.of(incident));
        when(projectAccessService.requireWritableProject(1L, authentication)).thenReturn(project);
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IncidentUpdateRequest request = new IncidentUpdateRequest();
        request.setStatus("resolved");
        request.setRootCause("Missing DB index");

        Incident updated = incidentService.updateIncident(10L, authentication, request);

        assertThat(updated.getStatus()).isEqualTo("RESOLVED");
        assertThat(updated.getResolvedAt()).isNotNull();
        assertThat(updated.getRootCause()).isEqualTo("Missing DB index");
    }

    @Test
    void shouldRejectUpdateWithInvalidStatus() {
        Project project = new Project();
        project.setId(1L);
        Incident incident = new Incident();
        incident.setId(10L);
        incident.setProject(project);

        when(incidentRepository.findById(10L)).thenReturn(Optional.of(incident));
        when(projectAccessService.requireWritableProject(1L, authentication)).thenReturn(project);

        IncidentUpdateRequest request = new IncidentUpdateRequest();
        request.setStatus("CLOSED");

        assertThatThrownBy(() -> incidentService.updateIncident(10L, authentication, request))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void shouldReturnIncidentWhenCallerHasAccess() {
        Project project = new Project();
        project.setId(1L);
        Incident incident = new Incident();
        incident.setId(10L);
        incident.setProject(project);
        incident.setTitle("Latency spike");

        when(incidentRepository.findById(10L)).thenReturn(Optional.of(incident));
        when(projectAccessService.requireAccessibleProject(1L, authentication)).thenReturn(project);

        Incident found = incidentService.getIncident(10L, authentication);

        assertThat(found.getTitle()).isEqualTo("Latency spike");
    }

    @Test
    void shouldCorrelateRecentDeploymentsOnlyForTheAccessibleProjectAndIncidentWindow() {
        LocalDateTime incidentTime = LocalDateTime.of(2026, 10, 5, 14, 0);
        Project project = new Project();
        project.setId(1L);
        Incident incident = new Incident();
        incident.setId(10L);
        incident.setProject(project);
        incident.setStartedAt(incidentTime);
        Deployment deployment = new Deployment();
        deployment.setId(20L);
        deployment.setProject(project);
        deployment.setVersion("v2.4.0");
        deployment.setStatus("SUCCESS");
        when(incidentRepository.findById(10L)).thenReturn(Optional.of(incident));
        when(projectAccessService.requireAccessibleProject(1L, authentication)).thenReturn(project);
        when(deploymentRepository.findCorrelatedDeployments(
                1L, incidentTime.minusHours(24), incidentTime, PageRequest.of(0, 10)))
                .thenReturn(List.of(deployment));

        var correlations = incidentService.correlateDeployments(10L, authentication);

        assertThat(correlations.incidentTime()).isEqualTo(incidentTime);
        assertThat(correlations.windowStart()).isEqualTo(incidentTime.minusHours(24));
        assertThat(correlations.deployments()).extracting("version").containsExactly("v2.4.0");
    }

    @Test
    void shouldPersistAiAnalysisOnIncident() {
        Project project = new Project();
        project.setId(1L);
        Incident incident = new Incident();
        incident.setId(10L);
        incident.setProject(project);
        incident.setTitle("Database timeouts");
        incident.setDescription("Checkout queries stalling");
        incident.setSeverity("HIGH");
        incident.setServiceName("order-api");
        incident.setStatus("OPEN");

        when(incidentRepository.findById(10L)).thenReturn(Optional.of(incident));
        when(projectAccessService.requireAccessibleProject(1L, authentication)).thenReturn(project);
        when(projectAccessService.requireWritableProject(1L, authentication)).thenReturn(project);
        when(aiAnalysisService.analyzeIncident(any())).thenReturn(new IncidentAnalysisResponse(
                "DATABASE",
                0.87,
                "Database performance degradation",
                java.util.List.of("Inspect slow queries", "Review latest deployment"),
                java.util.List.of("latency")
        ));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Incident analyzed = incidentService.analyzeIncident(10L, authentication);

        assertThat(analyzed.getRootCause()).isEqualTo("Database performance degradation");
        assertThat(analyzed.getRecommendations()).contains("Inspect slow queries");
        assertThat(analyzed.getConfidenceScore()).isEqualTo(0.87);
        assertThat(analyzed.getStatus()).isEqualTo("INVESTIGATING");
    }

    @Test
    void shouldRequireAdminToReadIncidentWithoutProject() {
        Incident orphan = new Incident();
        orphan.setId(11L);
        orphan.setProject(null);

        when(incidentRepository.findById(11L)).thenReturn(Optional.of(orphan));
        doThrow(new AccessDeniedException("admin only"))
                .when(projectAccessService).requireAdminForUnscopedResource(authentication);

        assertThatThrownBy(() -> incidentService.getIncident(11L, authentication))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldRequireAdminToUpdateIncidentWithoutProject() {
        Incident orphan = new Incident();
        orphan.setId(12L);
        orphan.setProject(null);

        when(incidentRepository.findById(12L)).thenReturn(Optional.of(orphan));
        doThrow(new AccessDeniedException("admin only"))
                .when(projectAccessService).requireAdminForUnscopedResource(authentication);

        IncidentUpdateRequest request = new IncidentUpdateRequest();
        request.setStatus("RESOLVED");

        assertThatThrownBy(() -> incidentService.updateIncident(12L, authentication, request))
                .isInstanceOf(AccessDeniedException.class);
    }
}
