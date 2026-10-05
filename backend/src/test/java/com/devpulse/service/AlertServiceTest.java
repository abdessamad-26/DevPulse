package com.devpulse.service;

import com.devpulse.entity.Alert;
import com.devpulse.entity.AlertRule;
import com.devpulse.entity.Project;
import com.devpulse.repository.AlertRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private ProjectAccessService projectAccessService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AlertService alertService;

    @Test
    void shouldAcknowledgeAlertWhenCallerHasAccessToProject() {
        Project project = new Project();
        project.setId(1L);
        AlertRule rule = new AlertRule();
        rule.setProject(project);
        Alert alert = new Alert();
        alert.setAlertRule(rule);
        alert.setStatus("OPEN");

        when(alertRepository.findById(5L)).thenReturn(Optional.of(alert));
        when(projectAccessService.requireAccessibleProject(1L, authentication)).thenReturn(project);
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Alert acknowledged = alertService.acknowledge(5L, authentication);

        assertThat(acknowledged.getStatus()).isEqualTo("ACKNOWLEDGED");
    }

    @Test
    void shouldRequireAdminToAcknowledgeAlertWhoseRuleHasNoProject() {
        AlertRule orphanRule = new AlertRule();
        orphanRule.setProject(null);
        Alert alert = new Alert();
        alert.setAlertRule(orphanRule);
        alert.setStatus("OPEN");

        when(alertRepository.findById(6L)).thenReturn(Optional.of(alert));
        doThrow(new AccessDeniedException("admin only"))
                .when(projectAccessService).requireAdminForUnscopedResource(authentication);

        assertThatThrownBy(() -> alertService.acknowledge(6L, authentication))
                .isInstanceOf(AccessDeniedException.class);
        verify(alertRepository, never()).save(any(Alert.class));
    }
}
