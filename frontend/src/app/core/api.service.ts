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
  projects(page = 0, size = 100): Observable<Page<Project>> {
    return this.http.get<Page<Project>>('/api/projects', { params: this.pageParams(page, size) });
  }
  createProject(body: ProjectPayload): Observable<Project> {
    return this.http.post<Project>('/api/projects', body);
  }

  // Services
  services(projectId: number, page = 0, size = 100): Observable<Page<ServiceItem>> {
    return this.http.get<Page<ServiceItem>>(`/api/projects/${projectId}/services`, { params: this.pageParams(page, size) });
  }
  createService(projectId: number, body: { name: string; type?: string; healthStatus?: string }): Observable<ServiceItem> {
    return this.http.post<ServiceItem>(`/api/projects/${projectId}/services`, body);
  }

  // Incidents
  incidents(projectId: number, page = 0, size = 100): Observable<Page<Incident>> {
    return this.http.get<Page<Incident>>('/api/incidents', { params: this.pageParams(page, size).set('projectId', projectId) });
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
  alerts(projectId: number, page = 0, size = 100): Observable<Page<Alert>> {
    return this.http.get<Page<Alert>>('/api/alerts', { params: this.pageParams(page, size).set('projectId', projectId) });
  }
  acknowledgeAlert(id: number): Observable<Alert> {
    return this.http.post<Alert>(`/api/alerts/${id}/ack`, {});
  }
  alertRules(projectId: number, page = 0, size = 100): Observable<Page<AlertRule>> {
    return this.http.get<Page<AlertRule>>(`/api/projects/${projectId}/alert-rules`, { params: this.pageParams(page, size) });
  }
  createAlertRule(projectId: number, body: AlertRulePayload): Observable<AlertRule> {
    return this.http.post<AlertRule>(`/api/projects/${projectId}/alert-rules`, body);
  }

  // Deployments
  deployments(projectId: number, page = 0, size = 100): Observable<Page<Deployment>> {
    return this.http.get<Page<Deployment>>('/api/deployments', { params: this.pageParams(page, size).set('projectId', projectId) });
  }
  recordDeployment(body: DeploymentPayload): Observable<Deployment> {
    return this.http.post<Deployment>('/api/deployments', body);
  }

  // Metrics
  metrics(projectId: number, page = 0, size = 100): Observable<Page<MetricPoint>> {
    return this.http.get<Page<MetricPoint>>(`/api/projects/${projectId}/metrics`, { params: this.pageParams(page, size) });
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
  chaosHistory(projectId: number, page = 0, size = 100): Observable<Page<ChaosSimulation>> {
    return this.http.get<Page<ChaosSimulation>>(`/api/projects/${projectId}/chaos`, { params: this.pageParams(page, size) });
  }
  triggerChaos(projectId: number, body: { action: string; targetService?: string }): Observable<ChaosSimulation> {
    return this.http.post<ChaosSimulation>(`/api/projects/${projectId}/chaos`, body);
  }

  private pageParams(page: number, size: number): HttpParams {
    return new HttpParams().set('page', page).set('size', size);
  }
}
