package com.devpulse.service;

import com.devpulse.dto.ServiceRequest;
import com.devpulse.entity.Project;
import com.devpulse.entity.ServiceEntity;
import com.devpulse.repository.ServiceRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServiceManagementService {

    private final ServiceRepository serviceRepository;
    private final ProjectAccessService projectAccessService;

    public ServiceManagementService(ServiceRepository serviceRepository, ProjectAccessService projectAccessService) {
        this.serviceRepository = serviceRepository;
        this.projectAccessService = projectAccessService;
    }

    public ServiceEntity createService(Long projectId, Authentication authentication, ServiceRequest request) {
        Project project = projectAccessService.requireAccessibleProject(projectId, authentication);

        ServiceEntity service = new ServiceEntity();
        service.setProject(project);
        service.setName(request.getName());
        service.setType(request.getType());
        if (request.getHealthStatus() != null && !request.getHealthStatus().isBlank()) {
            service.setHealthStatus(request.getHealthStatus());
        }
        return serviceRepository.save(service);
    }

    public List<ServiceEntity> listServices(Long projectId, Authentication authentication) {
        projectAccessService.requireAccessibleProject(projectId, authentication);
        return serviceRepository.findByProject_Id(projectId);
    }
}
