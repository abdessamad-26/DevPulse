# DevPulse API

This document describes the endpoints currently implemented in the repository. The backend API runs at `http://localhost:8080`; the AI service also exposes its own endpoints on port `8000`. Request validation errors use the backend's structured error response.

## Authentication

The backend issues JWT access and refresh tokens. Except for registration, login, refresh, logout, and the health endpoints, backend routes require an access token:

```http
Authorization: Bearer <access-token>
```

### `POST /api/auth/register`

Create an account. The password must contain at least eight characters.

```json
{
  "firstName": "Ada",
  "lastName": "Lovelace",
  "email": "ada@example.test",
  "password": "a-long-password"
}
```

Returns `201 Created` with the new user's `id`, `email`, `firstName`, `lastName`, and `role`.

### `POST /api/auth/login`

```json
{ "email": "ada@example.test", "password": "a-long-password" }
```

Returns an access `token`, a `refreshToken`, and a `user` summary.

### `POST /api/auth/refresh`

```json
{ "refreshToken": "<refresh-token>" }
```

Returns a new authentication response. **Refresh tokens are not rotated or revoked server-side yet**; logout is stateless and does not invalidate previously issued tokens.

### `POST /api/auth/logout`

Returns `204 No Content`. The client must discard its tokens; the server does not maintain a token blacklist.

## Projects and services

### `POST /api/projects`

Create a project (`ADMIN` or `DEVELOPER`).

```json
{
  "name": "Payments",
  "description": "Payments API",
  "repository": "https://example.test/payments",
  "environment": "development"
}
```

`name` and `environment` are required.

### `GET /api/projects`

List projects owned by the authenticated user.

### `GET /api/projects/{projectId}`

Get a project the authenticated user can access.

### `POST /api/projects/{projectId}/services`

Create a service in an accessible project (`ADMIN` or `DEVELOPER`).

```json
{ "name": "payments-api", "type": "http", "healthStatus": "HEALTHY" }
```

### `GET /api/projects/{projectId}/services`

List services in an accessible project.

Project access is currently based on project ownership (with administrative access); project membership management has not been implemented.

## Metrics and logs

### `POST /api/projects/{projectId}/metrics`

Ingest metric points (`ADMIN` or `DEVELOPER`).

```json
{
  "points": [
    {
      "serviceName": "payments-api",
      "metricName": "cpu_usage_percent",
      "value": 87.5,
      "unit": "%",
      "capturedAt": "2026-10-05T12:00:00"
    }
  ]
}
```

`metricName` and `value` are required. Ingested points are evaluated against enabled alert rules immediately.

### `GET /api/projects/{projectId}/metrics`

Query metrics the caller can access. Optional query parameters: `metric`, `from`, and `to` (ISO date-time). Results are returned as a list; the endpoint does not currently paginate.

### `POST /api/projects/{projectId}/logs`

Ingest log entries (`ADMIN` or `DEVELOPER`).

```json
{
  "entries": [
    {
      "serviceName": "payments-api",
      "environment": "production",
      "level": "ERROR",
      "message": "Payment provider timed out",
      "timestamp": "2026-10-05T12:00:00"
    }
  ]
}
```

Each entry requires `message`.

### `GET /api/projects/{projectId}/logs`

Search logs. Optional query parameters: `service`, `environment`, `level`, `q`, `from`, `to`, `page` (default `0`), and `size` (default `20`). Returns a Spring `Page` response.

## Incidents

### `POST /api/incidents`

Create an incident (`ADMIN` or `DEVELOPER`).

```json
{
  "projectId": 1,
  "serviceId": 2,
  "serviceName": "payments-api",
  "title": "Payment errors",
  "description": "Provider requests are timing out",
  "severity": "HIGH"
}
```

`projectId`, `title`, and `severity` are required. `serviceId`, `serviceName`, and `startedAt` are optional.

### `GET /api/incidents?projectId={projectId}`

List incidents for a project the caller can access.

### `GET /api/incidents/{id}`

Get an incident the caller can access.

### `PATCH /api/incidents/{id}`

Update incident status and analysis fields (`ADMIN` or `DEVELOPER`).

```json
{
  "status": "INVESTIGATING",
  "rootCause": "Provider timeout",
  "recommendations": "Check provider availability",
  "confidenceScore": 0.87
}
```

`status` is required.

### `POST /api/incidents/{id}/analyze`

Run the backend's incident-analysis flow for an accessible incident (`ADMIN` or `DEVELOPER`).

## Alert rules and alerts

### `POST /api/projects/{projectId}/alert-rules`

Create an alert rule (`ADMIN` or `DEVELOPER`).

```json
{
  "name": "High CPU",
  "metric": "cpu_usage_percent",
  "operator": ">",
  "threshold": 80,
  "duration": "5m",
  "severity": "HIGH",
  "enabled": true
}
```

### `GET /api/projects/{projectId}/alert-rules`

List alert rules for an accessible project.

### `GET /api/alerts?projectId={projectId}`

List alerts for an accessible project.

### `POST /api/alerts/{id}/ack`

Acknowledge an alert (`ADMIN` or `DEVELOPER`).

**Known limitation:** rule `duration` is stored but not evaluated. A single breaching metric point can open an alert; a sliding-window evaluator is not implemented.

## Deployments

### `POST /api/deployments`

Record a deployment (`ADMIN` or `DEVELOPER`).

```json
{
  "projectId": 1,
  "version": "v1.2.0",
  "commit": "abc123",
  "branch": "main",
  "environment": "production",
  "status": "SUCCESS",
  "startedAt": "2026-10-05T11:50:00",
  "finishedAt": "2026-10-05T11:51:00"
}
```

`projectId`, `version`, `environment`, and `status` are required. The deployment author is derived from the authenticated user.

### `GET /api/deployments?projectId={projectId}`

List deployments for an accessible project.

## Chaos simulations

### `POST /api/projects/{projectId}/chaos`

Create a simulated chaos event (`ADMIN` or `DEVELOPER`).

```json
{ "action": "LATENCY", "targetService": "payments-api" }
```

### `GET /api/projects/{projectId}/chaos`

List simulations for an accessible project.

These endpoints do **not** affect real infrastructure; they create records and related incidents. Simulations are blocked for projects marked `production` unless `CHAOS_ALLOW_PRODUCTION=true`.

## AI service

These endpoints are exposed by the FastAPI service at `http://localhost:8000`, not by the Spring API.

- `GET /health` — liveness response.
- `GET /ready` — readiness response.
- `POST /api/analysis/incidents` — deterministic incident analysis. The request contains `title`, `description`, optional `severity`, `service`, and `recent_logs`.
- `POST /api/analysis/anomalies` — z-score anomaly detection.

Anomaly request example:

```json
{ "history": [10, 11, 9, 10, 12], "value": 80, "threshold": 3 }
```

The response fields are `isAnomaly`, `zScore`, `mean`, `stdDev`, and `reason`. `zScore` is `null` when the value is anomalous against a constant history because the mathematical score is unbounded and JSON has no representation for infinity.

## Errors and current gaps

Backend errors use this envelope:

```json
{
  "timestamp": "2026-10-05T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/..."
}
```

There is no user-administration API or membership/invitation API yet. DTO coverage is incomplete, and only log search is paginated. Some response endpoints currently serialize persistence entities directly.
