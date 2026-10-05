package com.devpulse.service;

import com.devpulse.entity.Alert;
import com.devpulse.exception.ApiException;
import com.devpulse.repository.AlertRepository;
import com.devpulse.util.PageRequestSupport;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final ProjectAccessService projectAccessService;

    public AlertService(AlertRepository alertRepository, ProjectAccessService projectAccessService) {
        this.alertRepository = alertRepository;
        this.projectAccessService = projectAccessService;
    }

    public Page<Alert> listAlerts(Long projectId, Authentication authentication, int page, int size) {
        projectAccessService.requireAccessibleProject(projectId, authentication);
        return alertRepository.findByAlertRule_Project_IdOrderByCreatedAtDescIdDesc(
                projectId, PageRequestSupport.of(page, size));
    }

    public Alert acknowledge(Long alertId, Authentication authentication) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ApiException("Alert not found"));

        Long projectId = alert.getAlertRule().getProject() != null
                ? alert.getAlertRule().getProject().getId()
                : null;
        if (projectId != null) {
            projectAccessService.requireWritableProject(projectId, authentication);
        } else {
            projectAccessService.requireAdminForUnscopedResource(authentication);
        }

        alert.setStatus("ACKNOWLEDGED");
        return alertRepository.save(alert);
    }
}
