package com.devpulse.service;

import com.devpulse.dto.AuditLogResponse;
import com.devpulse.entity.AuditLog;
import com.devpulse.entity.Project;
import com.devpulse.entity.User;
import com.devpulse.exception.ApiException;
import com.devpulse.repository.AuditLogRepository;
import com.devpulse.repository.ProjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogRepository auditLogRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessService projectAccessService;

    public AuditLogService(AuditLogRepository auditLogRepository,
                          ProjectRepository projectRepository,
                          ProjectAccessService projectAccessService) {
        this.auditLogRepository = auditLogRepository;
        this.projectRepository = projectRepository;
        this.projectAccessService = projectAccessService;
    }

    @Transactional
    public void recordProject(Long projectId, User actor, String action, String entityType,
                              Long entityId, String details) {
        Project project = projectRepository.getReferenceById(projectId);
        record(project, actor, action, entityType, entityId, details);
    }

    @Transactional
    public void recordGlobal(User actor, String action, String entityType, Long entityId, String details) {
        record(null, actor, action, entityType, entityId, details);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> listProject(Long projectId, Authentication authentication,
                                               int page, int size) {
        projectAccessService.requireProjectManager(projectId, authentication);
        Pageable pageable = validatedPage(page, size);
        return auditLogRepository.findByProject_IdOrderByCreatedAtDescIdDesc(projectId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> listAll(Authentication authentication, int page, int size) {
        projectAccessService.requireGlobalAdmin(authentication);
        Pageable pageable = validatedPage(page, size);
        return auditLogRepository.findAllByOrderByCreatedAtDescIdDesc(pageable)
                .map(this::toResponse);
    }

    private Pageable validatedPage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException("Page must be non-negative and size must be between 1 and 100");
        }
        return PageRequest.of(page, size);
    }

    private void record(Project project, User actor, String action, String entityType,
                        Long entityId, String details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setProject(project);
        auditLog.setActor(actor);
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setDetails(details);
        auditLogRepository.save(auditLog);
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getProject() == null ? null : auditLog.getProject().getId(),
                auditLog.getActor() == null ? null : auditLog.getActor().getId(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getDetails(),
                auditLog.getCreatedAt());
    }
}
