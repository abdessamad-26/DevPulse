package com.devpulse.service;

import com.devpulse.dto.LogIngestRequest;
import com.devpulse.entity.LogEntry;
import com.devpulse.entity.Project;
import com.devpulse.repository.LogRepository;
import com.devpulse.util.PageRequestSupport;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class LogService {

    private final LogRepository logRepository;
    private final ProjectAccessService projectAccessService;

    public LogService(LogRepository logRepository, ProjectAccessService projectAccessService) {
        this.logRepository = logRepository;
        this.projectAccessService = projectAccessService;
    }

    public List<LogEntry> ingest(Long projectId, Authentication authentication, LogIngestRequest request) {
        Project project = projectAccessService.requireIngestionProject(projectId, authentication);

        return request.getEntries().stream().map(entry -> {
            LogEntry log = new LogEntry();
            log.setProject(project);
            log.setServiceName(entry.getServiceName());
            log.setEnvironment(entry.getEnvironment());
            log.setLevel(entry.getLevel());
            log.setMessage(entry.getMessage());
            if (entry.getTimestamp() != null) {
                log.setTimestamp(entry.getTimestamp());
            }
            return logRepository.save(log);
        }).toList();
    }

    public Page<LogEntry> search(Long projectId, Authentication authentication, String service, String environment,
                                  String level, String keyword, LocalDateTime from, LocalDateTime to, int page, int size) {
        projectAccessService.requireAccessibleProject(projectId, authentication);
        var pageable = PageRequestSupport.of(page, size,
                Sort.by(Sort.Direction.DESC, "timestamp").and(Sort.by(Sort.Direction.DESC, "id")));

        Specification<LogEntry> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("project").get("id"), projectId));
            if (service != null && !service.isBlank()) {
                predicates.add(cb.equal(root.get("serviceName"), service));
            }
            if (environment != null && !environment.isBlank()) {
                predicates.add(cb.equal(root.get("environment"), environment));
            }
            if (level != null && !level.isBlank()) {
                predicates.add(cb.equal(root.get("level"), level));
            }
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("message")), "%" + keyword.toLowerCase() + "%"));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return logRepository.findAll(spec, pageable);
    }
}
