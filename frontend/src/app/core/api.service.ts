import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Alert,
  AlertRule,
  AlertRulePayload,
  ChaosSimulation,
  Deployment,
  DeploymentPayload,
  Incident,
  IncidentCreatePayload,
  LogEntry,
  LogFilters,
  MetricIngestPoint,
  MetricPoint,
  Page,
  Project,
  ProjectPayload,
  ServiceItem,
} from './models';

/** Thin typed client over the DevPulse REST API (all paths relative, proxied in dev and in nginx). */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  // Projects
  projects(): Observable<Project[]> {
    return this.http.get<Project[]>('/api/projects');
  }
  createProject(body: ProjectPayload): Observable<Project> {
    return this.http.post<Project>('/api/projects', body);
  }

  // Services
  services(projectId: number): Observable<ServiceItem[]> {
    return this.http.get<ServiceItem[]>(`/api/projects/${projectId}/services`);
  }
  createService(projectId: number, body: { name: string; type?: string; healthStatus?: string }): Observable<ServiceItem> {
    return this.http.post<ServiceItem>(`/api/projects/${projectId}/services`, body);
  }

  // Incidents
  incidents(projectId: number): Observable<Incident[]> {
    return this.http.get<Incident[]>('/api/incidents', { params: { projectId } });
  }
  createIncident(body: IncidentCreatePayload): Observable<Incident> {
    return this.http.post<Incident>('/api/incidents', body);
  }
  updateIncidentStatus(id: number, status: string): Observable<Incident> {
    return this.http.patch<Incident>(`/api/incidents/${id}`, { status });
  }
  analyzeIncident(id: number): Observable<Incident> {
    return this.http.post<Incident>(`/api/incidents/${id}/analyze`, {});
  }

  // Alerts
  alerts(projectId: number): Observable<Alert[]> {
    return this.http.get<Alert[]>('/api/alerts', { params: { projectId } });
  }
  acknowledgeAlert(id: number): Observable<Alert> {
    return this.http.post<Alert>(`/api/alerts/${id}/ack`, {});
  }
  alertRules(projectId: number): Observable<AlertRule[]> {
    return this.http.get<AlertRule[]>(`/api/projects/${projectId}/alert-rules`);
  }
  createAlertRule(projectId: number, body: AlertRulePayload): Observable<AlertRule> {
    return this.http.post<AlertRule>(`/api/projects/${projectId}/alert-rules`, body);
  }

  // Deployments
  deployments(projectId: number): Observable<Deployment[]> {
    return this.http.get<Deployment[]>('/api/deployments', { params: { projectId } });
  }
  recordDeployment(body: DeploymentPayload): Observable<Deployment> {
    return this.http.post<Deployment>('/api/deployments', body);
  }

  // Metrics
  metrics(projectId: number): Observable<MetricPoint[]> {
    return this.http.get<MetricPoint[]>(`/api/projects/${projectId}/metrics`);
  }
  ingestMetrics(projectId: number, points: MetricIngestPoint[]): Observable<MetricPoint[]> {
    return this.http.post<MetricPoint[]>(`/api/projects/${projectId}/metrics`, { points });
  }

  // Logs
  logs(projectId: number, filters: LogFilters, page: number, size: number): Observable<Page<LogEntry>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (filters.service) params = params.set('service', filters.service);
    if (filters.level) params = params.set('level', filters.level);
    if (filters.q) params = params.set('q', filters.q);
    return this.http.get<Page<LogEntry>>(`/api/projects/${projectId}/logs`, { params });
  }

  // Chaos
  chaosHistory(projectId: number): Observable<ChaosSimulation[]> {
    return this.http.get<ChaosSimulation[]>(`/api/projects/${projectId}/chaos`);
  }
  triggerChaos(projectId: number, body: { action: string; targetService?: string }): Observable<ChaosSimulation> {
    return this.http.post<ChaosSimulation>(`/api/projects/${projectId}/chaos`, body);
  }
}
