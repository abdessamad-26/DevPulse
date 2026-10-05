package com.devpulse.service;

import com.devpulse.dto.AlertRuleRequest;
import com.devpulse.entity.AlertRule;
import com.devpulse.entity.Project;
import com.devpulse.exception.ApiException;
import com.devpulse.repository.AlertRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertRuleServiceTest {

    @Mock
    private AlertRuleRepository alertRuleRepository;

    @Mock
    private ProjectAccessService projectAccessService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AlertRuleService alertRuleService;

    private AlertRuleRequest validRequest() {
        AlertRuleRequest request = new AlertRuleRequest();
        request.setName("High CPU");
        request.setMetric("cpu_usage_percent");
        request.setOperator(">");
        request.setThreshold(80.0);
        request.setSeverity("high");
        return request;
    }

    @Test
    void shouldCreateRuleWithNormalizedSeverityAndDefaultEnabledTrue() {
        Project project = new Project();
        project.setId(1L);
        when(projectAccessService.requireAccessibleProject(1L, authentication)).thenReturn(project);
        when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AlertRule rule = alertRuleService.createRule(1L, authentication, validRequest());

        assertThat(rule.getSeverity()).isEqualTo("HIGH");
        assertThat(rule.isEnabled()).isTrue();
        assertThat(rule.getProject()).isEqualTo(project);
    }

    @Test
    void shouldRejectInvalidOperator() {
        Project project = new Project();
        project.setId(1L);
        when(projectAccessService.requireAccessibleProject(1L, authentication)).thenReturn(project);

        AlertRuleRequest request = validRequest();
        request.setOperator("~=");

        assertThatThrownBy(() -> alertRuleService.createRule(1L, authentication, request))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void shouldRejectInvalidSeverity() {
        Project project = new Project();
        project.setId(1L);
        when(projectAccessService.requireAccessibleProject(1L, authentication)).thenReturn(project);

        AlertRuleRequest request = validRequest();
        request.setSeverity("URGENT");

        assertThatThrownBy(() -> alertRuleService.createRule(1L, authentication, request))
                .isInstanceOf(ApiException.class);
    }
}
