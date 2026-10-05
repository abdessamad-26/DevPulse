// Types mirror the JSON actually produced by the Spring Boot backend
// (see backend/src/main/java/com/devpulse/entity and dto).

export type RoleName = 'ADMIN' | 'DEVELOPER' | 'VIEWER';

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string | null;
}

export interface AuthResponse {
  token: string;
  refreshToken: string;
  user: User;
}

export interface RegisterPayload {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

export interface Project {
  id: number;
  name: string;
  description: string | null;
  repository: string | null;
  environment: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectPayload {
  name: string;
  description?: string;
  repository?: string;
  environment: string;
}

export interface ServiceItem {
  id: number;
  projectId: number;
  name: string;
  type: string | null;
  healthStatus: string;
  createdAt: string;
}

export interface Incident {
  id: number;
  projectId: number | null;
  serviceId: number | null;
  title: string;
  description: string | null;
  severity: string;
  status: string;
  serviceName: string | null;
  startedAt: string | null;
  resolvedAt: string | null;
  detectedAt: string;
  rootCause: string | null;
  recommendations: string | null;
  confidenceScore: number | null;
  createdAt: string;
}

export interface IncidentDeploymentCorrelation {
  incidentId: number;
  incidentTime: string;
  windowStart: string;
  deployments: Deployment[];
}

export interface IncidentCreatePayload {
  projectId: number;
  serviceId?: number | null;
  title: string;
  description?: string;
  severity: string;
}

export interface Alert {
  id: number;
  message: string;
  severity: string;
  status: string;
  createdAt: string;
}

export interface AlertRule {
  id: number;
  projectId: number | null;
  name: string;
  metric: string;
  operator: string;
  threshold: number;
  duration: string | null;
  severity: string;
  enabled: boolean;
  createdAt: string;
}

export interface AlertRulePayload {
  name: string;
  metric: string;
  operator: string;
  threshold: number;
  duration?: string;
  severity: string;
  enabled: boolean;
}

export interface Deployment {
  id: number;
  projectId: number | null;
  version: string;
  commitHash: string | null;
  branch: string | null;
  environment: string;
  status: string;
  startedAt: string | null;
  finishedAt: string | null;
  author: string | null;
  createdAt: string;
}

export interface DeploymentPayload {
  projectId: number;
  version: string;
  commit?: string;
  branch?: string;
  environment: string;
  status: string;
}

export interface MetricPoint {
  id: number;
  projectId: number | null;
  serviceName: string | null;
  metricName: string;
  metricValue: number;
  unit: string | null;
  capturedAt: string;
}

export interface MetricIngestPoint {
  serviceName?: string;
  metricName: string;
  value: number;
  unit?: string;
}

export interface LogEntry {
  id: number;
  projectId: number | null;
  serviceName: string | null;
  environment: string | null;
  level: string | null;
  message: string;
  timestamp: string;
}

export interface LogFilters {
  service?: string;
  level?: string;
  q?: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ChaosSimulation {
  id: number;
  projectId: number | null;
  action: string;
  targetService: string | null;
  triggeredBy: string;
  status: string;
  resultingIncidentId: number | null;
  createdAt: string;
}
