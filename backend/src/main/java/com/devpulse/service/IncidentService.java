package com.devpulse.service;

import com.devpulse.dto.IncidentAnalysisRequest;
import com.devpulse.dto.IncidentAnalysisResponse;
import com.devpulse.dto.IncidentCreateRequest;
import com.devpulse.dto.IncidentUpdateRequest;
import com.devpulse.entity.Incident;
import com.devpulse.entity.Project;
import com.devpulse.entity.ServiceEntity;
import com.devpulse.exception.ApiException;
import com.devpulse.repository.IncidentRepository;
import com.devpulse.repository.ServiceRepository;
import com.devpulse.util.PageRequestSupport;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class IncidentService {

    private static final Set<String> VALID_SEVERITIES = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
    private static final Set<String> VALID_STATUSES = Set.of("OPEN", "INVESTIGATING", "RESOLVED");

    private final IncidentRepository incidentRepository;
    private final ServiceRepository serviceRepository;
    private final ProjectAccessService projectAccessService;
    private final AiAnalysisService aiAnalysisService;

    public IncidentService(IncidentRepository incidentRepository,
                            ServiceRepository serviceRepository,
                            ProjectAccessService projectAccessService,
                            AiAnalysisService aiAnalysisService) {
        this.incidentRepository = incidentRepository;
        this.serviceRepository = serviceRepository;
        this.projectAccessService = projectAccessService;
        this.aiAnalysisService = aiAnalysisService;
    }

    public Incident createIncident(Authentication authentication, IncidentCreateRequest request) {
        Project project = projectAccessService.requireWritableProject(request.getProjectId(), authentication);

        String severity = request.getSeverity().toUpperCase();
        if (!VALID_SEVERITIES.contains(severity)) {
            throw new ApiException("severity must be one of " + VALID_SEVERITIES);
        }

        ServiceEntity serviceRef = null;
        if (request.getServiceId() != null) {
            serviceRef = serviceRepository.findById(request.getServiceId())
                    .filter(s -> s.getProject().getId().equals(project.getId()))
                    .orElseThrow(() -> new ApiException("Service not found in this project"));
        }

        Incident incident = new Incident();
        incident.setProject(project);
        incident.setServiceRef(serviceRef);
        incident.setServiceName(serviceRef != null ? serviceRef.getName() : request.getServiceName());
        incident.setTitle(request.getTitle());
        incident.setDescription(request.getDescription());
        incident.setSeverity(severity);
        incident.setStatus("OPEN");
        incident.setStartedAt(request.getStartedAt() != null ? request.getStartedAt() : LocalDateTime.now());

        return incidentRepository.save(incident);
    }

    public Page<Incident> listIncidentsForProject(Long projectId, Authentication authentication, int page, int size) {
        projectAccessService.requireAccessibleProject(projectId, authentication);
        return incidentRepository.findByProject_IdOrderByCreatedAtDescIdDesc(
                projectId, PageRequestSupport.of(page, size));
    }

    public Incident updateIncident(Long incidentId, Authentication authentication, IncidentUpdateRequest request) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ApiException("Incident not found"));

        if (incident.getProject() != null) {
            projectAccessService.requireWritableProject(incident.getProject().getId(), authentication);
        } else {
            projectAccessService.requireAdminForUnscopedResource(authentication);
        }

        String status = request.getStatus().toUpperCase();
        if (!VALID_STATUSES.contains(status)) {
            throw new ApiException("status must be one of " + VALID_STATUSES);
        }

        incident.setStatus(status);
        if (request.getRootCause() != null) {
            incident.setRootCause(request.getRootCause());
        }
        if (request.getRecommendations() != null) {
            incident.setRecommendations(request.getRecommendations());
        }
        if (request.getConfidenceScore() != null) {
            incident.setConfidenceScore(request.getConfidenceScore());
        }
        if ("RESOLVED".equals(status) && incident.getResolvedAt() == null) {
            incident.setResolvedAt(LocalDateTime.now());
        }

        return incidentRepository.save(incident);
    }

    public Incident getIncident(Long incidentId, Authentication authentication) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ApiException("Incident not found", HttpStatus.NOT_FOUND));
        if (incident.getProject() != null) {
            projectAccessService.requireAccessibleProject(incident.getProject().getId(), authentication);
        } else {
            projectAccessService.requireAdminForUnscopedResource(authentication);
        }
        return incident;
    }

    /**
     * Runs the Python analysis service against this incident, then persists
     * root cause, recommendations and confidence. Failures are not invented:
     * if the AI service is down the caller receives an explicit error.
     */
    public Incident analyzeIncident(Long incidentId, Authentication authentication) {
        Incident incident = getIncident(incidentId, authentication);
        if (incident.getProject() != null) {
            projectAccessService.requireWritableProject(incident.getProject().getId(), authentication);
        } else {
            projectAccessService.requireAdminForUnscopedResource(authentication);
        }
        String description = incident.getDescription() == null || incident.getDescription().isBlank()
                ? incident.getTitle()
                : incident.getDescription();
        IncidentAnalysisResponse analysis = aiAnalysisService.analyzeIncident(new IncidentAnalysisRequest(
                incident.getTitle(),
                description,
                incident.getSeverity(),
                incident.getServiceName(),
                List.of()
        ));
        incident.setRootCause(analysis.rootCause());
        if (analysis.recommendations() != null) {
            incident.setRecommendations(String.join("\n", analysis.recommendations()));
        }
        incident.setConfidenceScore(analysis.confidence());
        if ("OPEN".equals(incident.getStatus())) {
            incident.setStatus("INVESTIGATING");
        }
        return incidentRepository.save(incident);
    }
}
