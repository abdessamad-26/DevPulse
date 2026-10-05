package com.devpulse.service;

import com.devpulse.dto.ChaosRequest;
import com.devpulse.entity.ChaosSimulation;
import com.devpulse.entity.Incident;
import com.devpulse.entity.Project;
import com.devpulse.exception.ApiException;
import com.devpulse.repository.ChaosSimulationRepository;
import com.devpulse.repository.IncidentRepository;
import com.devpulse.util.PageRequestSupport;
import org.springframework.data.domain.Page;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Simulates infrastructure failures for demo/testing purposes.
 *
 * Honest scope: there is no real Kubernetes cluster wired up yet (Phase 7),
 * so "Kill Pod" does not actually kill anything. What this really does is
 * record the simulation and open a matching {@link Incident} with a
 * severity/description appropriate to the chosen action, so the
 * detect -> investigate -> resolve workflow can be demonstrated end-to-end.
 * See docs/architecture.md#16.
 */
@Service
public class ChaosService {

    private static final Map<String, String> SEVERITY_BY_ACTION = Map.of(
            "KILL_POD", "CRITICAL",
            "DB_FAILURE", "CRITICAL",
            "CPU_LOAD", "HIGH",
            "LATENCY", "HIGH",
            "HTTP_500", "MEDIUM"
    );

    private static final Map<String, String> DESCRIPTION_BY_ACTION = Map.of(
            "KILL_POD", "Simulated pod termination",
            "DB_FAILURE", "Simulated database connectivity failure",
            "CPU_LOAD", "Simulated CPU load spike",
            "LATENCY", "Simulated API latency increase",
            "HTTP_500", "Simulated HTTP 500 error burst"
    );

    private final ChaosSimulationRepository chaosSimulationRepository;
    private final IncidentRepository incidentRepository;
    private final ProjectAccessService projectAccessService;
    private final boolean allowProduction;

    public ChaosService(ChaosSimulationRepository chaosSimulationRepository,
                         IncidentRepository incidentRepository,
                         ProjectAccessService projectAccessService,
                         @Value("${devpulse.chaos.allow-production:false}") boolean allowProduction) {
        this.chaosSimulationRepository = chaosSimulationRepository;
        this.incidentRepository = incidentRepository;
        this.projectAccessService = projectAccessService;
        this.allowProduction = allowProduction;
    }

    public ChaosSimulation trigger(Long projectId, Authentication authentication, ChaosRequest request) {
        Project project = projectAccessService.requireWritableProject(projectId, authentication);

        String action = request.getAction().toUpperCase();
        if (!SEVERITY_BY_ACTION.containsKey(action)) {
            throw new ApiException("action must be one of " + SEVERITY_BY_ACTION.keySet());
        }

        if ("production".equalsIgnoreCase(project.getEnvironment()) && !allowProduction) {
            throw new ApiException(
                    "Chaos simulations are disabled on production projects by default. " +
                    "Set CHAOS_ALLOW_PRODUCTION=true to override (not recommended).");
        }

        Incident incident = new Incident();
        incident.setProject(project);
        incident.setServiceName(request.getTargetService());
        incident.setTitle(DESCRIPTION_BY_ACTION.get(action) + (request.getTargetService() != null ? " on " + request.getTargetService() : ""));
        incident.setDescription("Triggered manually via chaos engineering simulation ("
                + authentication.getName() + "). This incident was NOT caused by a real failure.");
        incident.setSeverity(SEVERITY_BY_ACTION.get(action));
        incident.setStatus("OPEN");
        incident.setStartedAt(LocalDateTime.now());
        Incident savedIncident = incidentRepository.save(incident);

        ChaosSimulation simulation = new ChaosSimulation();
        simulation.setProject(project);
        simulation.setAction(action);
        simulation.setTargetService(request.getTargetService());
        simulation.setTriggeredBy(authentication.getName());
        simulation.setStatus("COMPLETED");
        simulation.setResultingIncident(savedIncident);

        return chaosSimulationRepository.save(simulation);
    }

    public Page<ChaosSimulation> history(Long projectId, Authentication authentication, int page, int size) {
        projectAccessService.requireAccessibleProject(projectId, authentication);
        return chaosSimulationRepository.findByProject_IdOrderByCreatedAtDescIdDesc(
                projectId, PageRequestSupport.of(page, size));
    }
}
