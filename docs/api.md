# DevPulse — API Reference (Overview)

This document lists the core API endpoints for DevPulse. Full OpenAPI specs will be generated from controller annotations and placed under `docs/openapi.yaml`.

Base path: `/api`

## Auth
- POST `/api/auth/register` — Register a new user
  - Body: `{ "username": "", "email": "", "password": "" }`
  - Response: `201 Created` `{ "id": ..., "username": ..., "email": ... }`

- POST `/api/auth/login` — Login
  - Body: `{ "username": "", "password": "" }`
  - Response: `200 OK` `{ "accessToken": "...", "refreshToken": "..." }`

- POST `/api/auth/refresh` — Refresh access token
  - Body: `{ "refreshToken": "..." }`
  - Response: `200 OK` `{ "accessToken": "..." }`

- POST `/api/auth/logout` — Invalidate refresh token
  - Auth: Bearer token

## Users (ADMIN)
- GET `/api/users` — List users (pagination)
- GET `/api/users/{id}` — Get user
- POST `/api/users` — Create user
- PATCH `/api/users/{id}` — Update user

## Projects
- GET `/api/projects` — List projects (filter: owner, name)
- POST `/api/projects` — Create project
  - Body: `{ "name": "...", "description": "...", "repository": "..." }`
- GET `/api/projects/{id}` — Get project detail
- PATCH `/api/projects/{id}` — Update project

## Services
- GET `/api/projects/{projectId}/services` — List services
- POST `/api/projects/{projectId}/services` — Register service

## Metrics (ingest & query)
- POST `/api/services/{id}/metrics` — Ingest metrics (bulk)
  - Body: `[{ "metric": "http_requests_total", "value": 1, "timestamp": 163... }]`

- GET `/api/services/{id}/metrics` — Query metrics (params: metric, from, to, step)

## Logs (ingest & search)
- POST `/api/services/{id}/logs` — Ingest logs (bulk)
- GET `/api/services/{id}/logs` — Search logs (params: level, q, from, to, page)

## Incidents
- GET `/api/incidents` — List incidents (filters)
- POST `/api/incidents` — Create incident (used by detection engine)
- GET `/api/incidents/{id}` — Get incident
- PATCH `/api/incidents/{id}` — Update status / add comment

## Alerts & Alert Rules
- GET `/api/alert-rules` — List rules
- POST `/api/alert-rules` — Create rule
- POST `/api/alerts/{id}/ack` — Acknowledge alert

## Deployments
- POST `/api/deployments` — Record a deployment event
- GET `/api/deployments/{serviceId}` — List deployments for a service

## Chaos (admin-only, gated)
- POST `/api/chaos/simulate` — Trigger a simulation (requires ADMIN and demo mode enabled)

## AI
- POST `/api/ai/analyze` — Analyze an incident or dataset
  - Body: `{ "incidentId": "..." }` or `{ "metrics": [...], "logs": [...] }`
  - Response: `{ "summary": "...", "rootCauseCandidates": [...], "recommendations": [...], "confidence": 0.87 }`

## Errors
All endpoints return structured error envelopes:
```json
{
  "timestamp": "...",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: ...",
  "path": "/api/..."
}
```

## Authentication
- All endpoints except `/auth/*` require `Authorization: Bearer <token>`
- Role-based access enforced in service layer

## Pagination
- Use standard `page` & `size` query params; responses include `totalElements`, `totalPages`, `page`, `size`.


> Note: This is an overview. I will generate a full OpenAPI YAML from controllers (or hand-author `docs/openapi.yaml`) next.