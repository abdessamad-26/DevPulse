package com.devpulse.configuration;

import com.devpulse.entity.*;
import com.devpulse.repository.*;
import com.devpulse.service.AlertEvaluationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemoDataSeederTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ProjectRepository projectRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private IncidentRepository incidentRepository;
    @Mock private DeploymentRepository deploymentRepository;
    @Mock private AlertRuleRepository alertRuleRepository;
    @Mock private MetricRepository metricRepository;
    @Mock private AlertEvaluationService alertEvaluationService;

    private DemoDataSeeder seeder(boolean enabled) {
        return new DemoDataSeeder(enabled, userRepository, roleRepository, passwordEncoder, projectRepository,
                serviceRepository, incidentRepository, deploymentRepository, alertRuleRepository, metricRepository,
                alertEvaluationService);
    }

    @Test
    void shouldDoNothingWhenDemoModeIsDisabled() {
        seeder(false).run();

        verifyNoInteractions(userRepository, projectRepository, serviceRepository, incidentRepository,
                deploymentRepository, alertRuleRepository, metricRepository, alertEvaluationService);
    }

    @Test
    void shouldSkipSeedingWhenDemoUserAlreadyExists() {
        when(userRepository.existsByEmail("demo@devpulse.local")).thenReturn(true);

        seeder(true).run();

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(projectRepository);
    }

    @Test
    void shouldSeedFullDemoDatasetWhenEnabledAndNotYetSeeded() {
        when(userRepository.existsByEmail("demo@devpulse.local")).thenReturn(false);

        Role developerRole = new Role();
        developerRole.setName("DEVELOPER");
        when(roleRepository.findByName("DEVELOPER")).thenReturn(Optional.of(developerRole));
        when(passwordEncoder.encode(any())).thenReturn("encoded");

        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });
        when(serviceRepository.save(any(ServiceEntity.class))).thenAnswer(inv -> {
            ServiceEntity s = inv.getArgument(0);
            s.setId(100L);
            return s;
        });
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deploymentRepository.save(any(Deployment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(inv -> inv.getArgument(0));
        when(metricRepository.save(any(Metric.class))).thenAnswer(inv -> inv.getArgument(0));

        seeder(true).run();

        verify(userRepository, times(1)).save(any(User.class));
        verify(projectRepository, times(1)).save(any(Project.class));
        verify(serviceRepository, times(3)).save(any(ServiceEntity.class));
        verify(incidentRepository, times(2)).save(any(Incident.class));
        verify(deploymentRepository, times(3)).save(any(Deployment.class));
        verify(alertRuleRepository, times(1)).save(any(AlertRule.class));
        // 1 breaching metric (evaluated) + 6 historical points
        verify(metricRepository, times(7)).save(any(Metric.class));

        ArgumentCaptor<Metric> evaluatedMetric = ArgumentCaptor.forClass(Metric.class);
        verify(alertEvaluationService, times(1)).evaluate(evaluatedMetric.capture());
        assertThat(evaluatedMetric.getValue().getMetricValue()).isEqualTo(91.3);
    }
}
