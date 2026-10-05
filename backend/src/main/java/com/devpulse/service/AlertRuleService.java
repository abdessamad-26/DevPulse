package com.devpulse.service;

import com.devpulse.dto.AlertRuleRequest;
import com.devpulse.entity.AlertRule;
import com.devpulse.entity.Project;
import com.devpulse.exception.ApiException;
import com.devpulse.repository.AlertRuleRepository;
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

        AlertRule rule = new AlertRule();
        rule.setProject(project);
        rule.setName(request.getName());
        rule.setMetric(request.getMetric());
        rule.setOperator(request.getOperator());
        rule.setThreshold(request.getThreshold());
        rule.setDuration(request.getDuration());
        rule.setSeverity(severity);
        rule.setEnabled(request.getEnabled() == null || request.getEnabled());

        return alertRuleRepository.save(rule);
    }

    public List<AlertRule> listRules(Long projectId, Authentication authentication) {
        projectAccessService.requireAccessibleProject(projectId, authentication);
        return alertRuleRepository.findByProject_Id(projectId);
    }
}
