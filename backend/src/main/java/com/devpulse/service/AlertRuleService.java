package com.devpulse.service;

import com.devpulse.dto.AlertRuleRequest;
import com.devpulse.entity.AlertRule;
import com.devpulse.entity.Project;
import com.devpulse.exception.ApiException;
import com.devpulse.repository.AlertRuleRepository;
import com.devpulse.util.AlertDuration;
import com.devpulse.util.PageRequestSupport;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class AlertRuleService {

    private static final Set<String> VALID_OPERATORS = Set.of(">", "<", ">=", "<=", "==");
    private static final Set<String> VALID_SEVERITIES = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");

    private final AlertRuleRepository alertRuleRepository;
    private final ProjectAccessService projectAccessService;

    public AlertRuleService(AlertRuleRepository alertRuleRepository, ProjectAccessService projectAccessService) {
        this.alertRuleRepository = alertRuleRepository;
        this.projectAccessService = projectAccessService;
    }

    public AlertRule createRule(Long projectId, Authentication authentication, AlertRuleRequest request) {
        Project project = projectAccessService.requireWritableProject(projectId, authentication);

        if (!VALID_OPERATORS.contains(request.getOperator())) {
            throw new ApiException("operator must be one of " + VALID_OPERATORS);
        }
        String severity = request.getSeverity().toUpperCase();
        if (!VALID_SEVERITIES.contains(severity)) {
            throw new ApiException("severity must be one of " + VALID_SEVERITIES);
        }
        AlertDuration.parse(request.getDuration());

        AlertRule rule = new AlertRule();
        rule.setProject(project);
        rule.setName(request.getName());
        rule.setMetric(request.getMetric());
        rule.setOperator(request.getOperator());
        rule.setThreshold(request.getThreshold());
        rule.setDuration(request.getDuration() == null ? null : request.getDuration().trim());
        rule.setSeverity(severity);
        rule.setEnabled(request.getEnabled() == null || request.getEnabled());

        return alertRuleRepository.save(rule);
    }

    public Page<AlertRule> listRules(Long projectId, Authentication authentication, int page, int size) {
        projectAccessService.requireAccessibleProject(projectId, authentication);
        return alertRuleRepository.findByProject_IdOrderByCreatedAtDescIdDesc(
                projectId, PageRequestSupport.of(page, size));
    }
}
