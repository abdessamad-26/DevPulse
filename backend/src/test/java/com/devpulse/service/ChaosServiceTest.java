package com.devpulse.service;

import com.devpulse.dto.ChaosRequest;
import com.devpulse.entity.ChaosSimulation;
import com.devpulse.entity.Incident;
import com.devpulse.entity.Project;
import com.devpulse.exception.ApiException;
import com.devpulse.repository.ChaosSimulationRepository;
import com.devpulse.repository.IncidentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChaosServiceTest {

    @Mock
    private ChaosSimulationRepository chaosSimulationRepository;

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private ProjectAccessService projectAccessService;

    @Mock
    private Authentication authentication;

    private Project projectWithEnvironment(String environment) {
        Project project = new Project();
        project.setId(1L);
        project.setEnvironment(environment);
        return project;
    }

    @Test
    void shouldTriggerSimulationAndOpenIncidentForNonProductionProject() {
        ChaosService chaosService = new ChaosService(chaosSimulationRepository, incidentRepository, projectAccessService, false);

        Project project = projectWithEnvironment("staging");
        when(projectAccessService.requireWritableProject(1L, authentication)).thenReturn(project);
        when(authentication.getName()).thenReturn("dev@example.com");
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chaosSimulationRepository.save(any(ChaosSimulation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChaosRequest request = new ChaosRequest();
        request.setAction("kill_pod");
        request.setTargetService("order-api");

        ChaosSimulation simulation = chaosService.trigger(1L, authentication, request);

        assertThat(simulation.getAction()).isEqualTo("KILL_POD");
        assertThat(simulation.getStatus()).isEqualTo("COMPLETED");
        assertThat(simulation.getTriggeredBy()).isEqualTo("dev@example.com");
        assertThat(simulation.getResultingIncident()).isNotNull();
        assertThat(simulation.getResultingIncident().getSeverity()).isEqualTo("CRITICAL");
        assertThat(simulation.getResultingIncident().getStatus()).isEqualTo("OPEN");
    }

    @Test
    void shouldRejectSimulationOnProductionByDefault() {
        ChaosService chaosService = new ChaosService(chaosSimulationRepository, incidentRepository, projectAccessService, false);

        Project project = projectWithEnvironment("production");
        when(projectAccessService.requireWritableProject(1L, authentication)).thenReturn(project);

        ChaosRequest request = new ChaosRequest();
        request.setAction("cpu_load");

        assertThatThrownBy(() -> chaosService.trigger(1L, authentication, request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("disabled on production");
    }

    @Test
    void shouldAllowProductionWhenExplicitlyOverridden() {
        ChaosService chaosService = new ChaosService(chaosSimulationRepository, incidentRepository, projectAccessService, true);

        Project project = projectWithEnvironment("production");
        when(projectAccessService.requireWritableProject(1L, authentication)).thenReturn(project);
        when(authentication.getName()).thenReturn("admin@example.com");
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chaosSimulationRepository.save(any(ChaosSimulation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChaosRequest request = new ChaosRequest();
        request.setAction("http_500");

        ChaosSimulation simulation = chaosService.trigger(1L, authentication, request);

        assertThat(simulation.getAction()).isEqualTo("HTTP_500");
    }

    @Test
    void shouldRejectUnknownAction() {
        ChaosService chaosService = new ChaosService(chaosSimulationRepository, incidentRepository, projectAccessService, false);

        Project project = projectWithEnvironment("development");
        when(projectAccessService.requireWritableProject(1L, authentication)).thenReturn(project);

        ChaosRequest request = new ChaosRequest();
        request.setAction("DELETE_EVERYTHING");

        assertThatThrownBy(() -> chaosService.trigger(1L, authentication, request))
                .isInstanceOf(ApiException.class);
    }
}
