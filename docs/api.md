# DevPulse API

This document describes the endpoints currently implemented in the repository. The backend API runs at `http://localhost:8080`; the AI service also exposes its own endpoints on port `8000`. Request validation errors use the backend's structured error response.

## Pagination and response models

Collection endpoints for projects, services, metrics, logs, incidents, alert
rules, alerts, deployments, and chaos history return a Spring `Page` envelope.
Use `page` (zero-based, default `0`) and `size` (default `20`, maximum `100`).
The response contains `content`, `totalElements`, `totalPages`, `number`, and
`size`. Responses use explicit API DTOs rather than serializing persistence
entities.

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

Returns a new authentication response with a new access token and a rotated refresh token. Each refresh token is single-use and stored server-side only as a SHA-256 hash. Reusing an already rotated token revokes all active refresh tokens in that login session's family; the client must sign in again.

### `POST /api/auth/logout`

Optionally send the current refresh token:

```json
{ "refreshToken": "<refresh-token>" }
```

Returns `204 No Content` and revokes all active refresh tokens in that login
session's family. A request without a body remains accepted for compatibility,
but cannot revoke a server-side session. The client should always send its
refresh token and discard both tokens. Already-issued access tokens remain
valid until their short expiration because access-token revocation is not
implemented.

### Audit logs

The API records successful logins, refresh-token rotations and reuse attempts,
logout, project-member changes, and ingestion-key creation/revocation. Audit
records contain actor/entity identifiers and controlled metadata only; they
never store passwords, JWTs, or ingestion-key secrets.

#### `GET /api/projects/{projectId}/audit-logs`

Lists a project's events, newest first, as a Spring `Page`. Only the project
owner and global `ADMIN` can read it. Optional query parameters are `page`
(default `0`) and `size` (default `20`, maximum `100`).

#### `GET /api/audit-logs`

Lists all audit events, newest first, as a Spring `Page`. This endpoint is
restricted to global `ADMIN` users. It accepts the same `page` and `size`
parameters.

## Projects and services

### `POST /api/projects`

Create a project (global `ADMIN` or `DEVELOPER`).

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

List projects owned by or shared with the authenticated user.

### `GET /api/projects/{projectId}`

Get a project the authenticated user can access.

### Project members

Project owners and `ADMIN` users can manage project membership. A member must
already have an account, and project roles are independent of their global role.
`DEVELOPER` can read and modify project resources; `VIEWER` can read them only.
The project owner retains full access and is included in the member list with
the `OWNER` role.

#### `GET /api/projects/{projectId}/members`

List the project owner and members. Any project member may read this list.

#### `POST /api/projects/{projectId}/members`

Add an existing account by email.

```json
{ "email": "developer@example.com", "role": "DEVELOPER" }
```

#### `PUT /api/projects/{projectId}/members/{userId}`

Change a member's project role.

```json
{ "role": "VIEWER" }
```

#### `DELETE /api/projects/{projectId}/members/{userId}`

Remove a project member. The project owner cannot be removed.

### `POST /api/projects/{projectId}/services`

Create a service in a project the caller can modify (project owner, global
`ADMIN`, or project member with the `DEVELOPER` role).

```json
{ "name": "payments-api", "type": "http", "healthStatus": "HEALTHY" }
```

### `GET /api/projects/{projectId}/services`

List services in an accessible project.

## Metrics and logs

### Ingestion API keys

Project owners and global `ADMIN` users can create, list, and revoke project
ingestion keys. Keys are restricted to `POST` metric and log ingestion for the
project that owns the key; they cannot query data or access any other API.
Secrets are returned only once at creation. Store them securely and send them
in the `X-API-Key` header. Revoking a key takes effect immediately.

#### `POST /api/projects/{projectId}/ingestion-keys`

```json
{ "name": "production collector" }
```

The response contains the secret `key` once, along with its display prefix.

#### `GET /api/projects/{projectId}/ingestion-keys`

List key names, prefixes, creation times, and last-used times. The secret is
never returned by this endpoint.

#### `DELETE /api/projects/{projectId}/ingestion-keys/{keyId}`

Revoke a project ingestion key. Returns `204 No Content`.

### `POST /api/projects/{projectId}/metrics`

Ingest metric points as the project owner, global `ADMIN`, or a project member with the `DEVELOPER` role.
An ingestion key scoped to this project may also authenticate this request with
the `X-API-Key` header.

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

`metricName` and `value` are required. Ingested points are evaluated against
enabled alert rules. Rules without a `duration` are evaluated immediately;
rules with a duration use the sample timestamps to require a sustained breach.

### `GET /api/projects/{projectId}/metrics`

Query metrics the caller can access. Optional query parameters: `metric`,
`from`, `to` (ISO date-time), `page`, and `size`. Results are returned as a
paginated response.

### `POST /api/projects/{projectId}/logs`

Ingest log entries as the project owner, global `ADMIN`, or a project member with the `DEVELOPER` role.
An ingestion key scoped to this project may also authenticate this request with
the `X-API-Key` header.

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

Search logs. Optional query parameters: `service`, `environment`, `level`, `q`,
`from`, `to`, `page`, and `size`. Results are ordered newest first and returned
as a paginated response.

## Incidents

### `POST /api/incidents`

Create an incident as the project owner, global `ADMIN`, or a project member with the `DEVELOPER` role.

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

List incidents for a project the caller can access. Accepts `page` and `size`.

### `GET /api/incidents/{id}`

Get an incident the caller can access.

### `PATCH /api/incidents/{id}`

Update incident status and analysis fields as the project owner, global `ADMIN`, or a project member with the `DEVELOPER` role.

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

Run the backend's incident-analysis flow as the project owner, global `ADMIN`, or a project member with the `DEVELOPER` role.

## Alert rules and alerts

### `POST /api/projects/{projectId}/alert-rules`

Create an alert rule as the project owner, global `ADMIN`, or a project member with the `DEVELOPER` role.

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

List alert rules for an accessible project. Accepts `page` and `size`.

### `GET /api/alerts?projectId={projectId}`

List alerts for an accessible project. Accepts `page` and `size`.

### `POST /api/alerts/{id}/ack`

Acknowledge an alert as the project owner, global `ADMIN`, or a project member with the `DEVELOPER` role.

Rule `duration` accepts a positive compact value (`500ms`, `30s`, `5m`,
`2h`, `1d`) or an ISO-8601 duration (`PT5M`). For a timed rule, the latest
sample and every observed sample in the preceding window must breach; at least
one breaching sample at or before the window start is required. Any observed
non-breaching sample interrupts the window. Missing samples are not synthesized
or treated as failures. Rules without a duration retain immediate evaluation.
An `OPEN` or `ACKNOWLEDGED` alert is automatically marked `RESOLVED` when the
next ingested sample for its metric no longer breaches the rule. A later
breach creates a new alert. Resolved alerts cannot be acknowledged.

### Alert webhook notifications

Set the backend environment variable `ALERT_WEBHOOK_URL` to an operator-managed
HTTP(S) endpoint to receive `POST` notifications for `alert.opened` and
`alert.resolved` events. Leave it empty to disable delivery. Each JSON payload
contains the event name, alert and rule identifiers, project ID, threshold,
observed value, severity, status, message, and event timestamp. The backend
uses a bounded asynchronous queue; delivery failures and queue saturation are
logged and do not fail metric ingestion. Delivery is best-effort (there is no
persistent retry/outbox), and HTTP requests have bounded connect/read timeouts.
Keep the URL operator-controlled and use HTTPS for remote endpoints.

## Deployments

### `POST /api/deployments`

Record a deployment as the project owner, global `ADMIN`, or a project member with the `DEVELOPER` role.

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

List deployments for an accessible project. Accepts `page` and `size`.

## Chaos simulations

### `POST /api/projects/{projectId}/chaos`

Create a simulated chaos event as the project owner, global `ADMIN`, or a project member with the `DEVELOPER` role.

```json
{ "action": "LATENCY", "targetService": "payments-api" }
```

### `GET /api/projects/{projectId}/chaos`

List simulations for an accessible project. Accepts `page` and `size`.

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

There is no user-administration API or invitation API yet. Collection endpoints
use bounded page requests and explicit response DTOs; ingestion-key and member
collections remain intentionally small project-scoped lists.
