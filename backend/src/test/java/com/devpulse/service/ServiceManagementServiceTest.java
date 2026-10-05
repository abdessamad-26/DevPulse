package com.devpulse.service;

import com.devpulse.dto.ServiceRequest;
import com.devpulse.entity.Project;
import com.devpulse.entity.ServiceEntity;
import com.devpulse.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceManagementServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private ProjectAccessService projectAccessService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ServiceManagementService serviceManagementService;

    @Test
    void shouldCreateServiceWhenUserHasProjectAccess() {
        Project project = new Project();
        project.setId(1L);

        when(projectAccessService.requireWritableProject(1L, authentication)).thenReturn(project);
        when(serviceRepository.save(any(ServiceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceRequest request = new ServiceRequest();
        request.setName("billing-api");
        request.setType("http");

        ServiceEntity created = serviceManagementService.createService(1L, authentication, request);

        assertThat(created.getName()).isEqualTo("billing-api");
        assertThat(created.getHealthStatus()).isEqualTo("HEALTHY");
        assertThat(created.getProject()).isEqualTo(project);
    }

    @Test
    void shouldRejectServiceCreationWhenUserHasNoAccess() {
        when(projectAccessService.requireWritableProject(1L, authentication))
                .thenThrow(new AccessDeniedException("no access"));

        ServiceRequest request = new ServiceRequest();
        request.setName("billing-api");

        assertThatThrownBy(() -> serviceManagementService.createService(1L, authentication, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldListServicesForAccessibleProject() {
        ServiceEntity service = new ServiceEntity();
        service.setName("billing-api");

        when(serviceRepository.findByProject_Id(1L)).thenReturn(List.of(service));

        List<ServiceEntity> services = serviceManagementService.listServices(1L, authentication);

        assertThat(services).hasSize(1);
        assertThat(services.get(0).getName()).isEqualTo("billing-api");
    }
}
