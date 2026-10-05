package com.devpulse.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Records a chaos-engineering simulation triggered by a user. IMPORTANT: this
 * does NOT talk to a real Kubernetes cluster (Phase 7 infrastructure is not
 * built yet) - it is a simulated effect that opens a matching {@link Incident}
 * so the incident-response workflow (detection -> investigation -> resolution)
 * can be demonstrated end-to-end without real infrastructure. See
 * docs/architecture.md#16 for the honest scope of what "chaos" means here.
 */
@Entity
@Table(name = "chaos_simulations")
public class ChaosSimulation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /** KILL_POD | CPU_LOAD | LATENCY | HTTP_500 | DB_FAILURE */
    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "target_service", length = 150)
    private String targetService;

    @Column(name = "triggered_by", nullable = false, length = 255)
    private String triggeredBy;

    @Column(nullable = false, length = 50)
    private String status = "TRIGGERED";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resulting_incident_id")
    private Incident resultingIncident;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ChaosSimulation() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @JsonIgnore
    public Project getProject() { return project; }

    @JsonProperty("projectId")
    public Long getProjectId() { return project == null ? null : project.getId(); }
    public void setProject(Project project) { this.project = project; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTargetService() { return targetService; }
    public void setTargetService(String targetService) { this.targetService = targetService; }

    public String getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @JsonIgnore
    public Incident getResultingIncident() { return resultingIncident; }

    @JsonProperty("resultingIncidentId")
    public Long getResultingIncidentId() { return resultingIncident == null ? null : resultingIncident.getId(); }
    public void setResultingIncident(Incident resultingIncident) { this.resultingIncident = resultingIncident; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
