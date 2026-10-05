# DevPulse — API Reference (Overview)

This document lists the core API endpoints for DevPulse. Full OpenAPI specs will be generated from controller annotations and placed under `docs/openapi.yaml`.

Base path: `/api`

## Auth ✅ implémenté et testé (voir `AuthController`, `AuthFlowIntegrationTest`)
- POST `/api/auth/register` — Register a new user
  - Body: `{ "firstName": "", "lastName": "", "email": "", "password": "" }` (password ≥ 8 caractères)
  - Response: `201 Created` `{ "id": ..., "email": ..., "firstName": ..., "lastName": ..., "role": "DEVELOPER" }`

- POST `/api/auth/login` — Login
  - Body: `{ "email": "", "password": "" }`
  - Response: `200 OK` `{ "token": "...", "refreshToken": "...", "user": { ... } }`

- POST `/api/auth/refresh` — Refresh access token
  - Body: `{ "refreshToken": "..." }`
  - Response: `200 OK` `{ "token": "...", "refreshToken": "...", "user": { ... } }`
  - ⚠️ Pas de rotation : le même refresh token est renvoyé tant qu'il est valide (voir `docs/architecture.md#16`).

- POST `/api/auth/logout` — 204 No Content
  - ⚠️ No-op côté serveur (JWT stateless, pas de blacklist) : le client doit simplement supprimer ses tokens.

## Users (ADMIN)
- GET `/api/users` — List users (pagination)
- GET `/api/users/{id}` — Get user
- POST `/api/users` — Create user
- PATCH `/api/users/{id}` — Update user

## Projects ✅ implementé et testé (partiellement — voir `ProjectController`)
- GET `/api/projects` — List **my own** projects (Auth: Bearer requis, tous rôles)
- POST `/api/projects` — Create project (Auth: Bearer requis, rôle `ADMIN` ou `DEVELOPER` uniquement — `VIEWER` reçoit 403)
  - Body: `{ "name": "...", "description": "...", "repository": "...", "environment": "development|staging|production" }`
- GET `/api/projects/{id}` — ❌ pas encore implémenté
- PATCH `/api/projects/{id}` — ❌ pas encore implémenté

## Services ✅ implémenté et testé (voir `ServiceController`, `ProjectResourcesIntegrationTest`)
- GET `/api/projects/{projectId}/services` — List services for a project (Auth: Bearer requis, doit être propriétaire du projet ou ADMIN)
- POST `/api/projects/{projectId}/services` — Register a service (rôle `ADMIN` ou `DEVELOPER`, doit posséder le projet)
  - Body: `{ "name": "...", "type": "http|worker|...", "healthStatus": "HEALTHY" }` (healthStatus optionnel, défaut `HEALTHY`)

## Metrics (ingest & query) ✅ implémenté et testé (voir `MetricController`, `MetricServiceTest`, `ObservabilityIntegrationTest`)
- POST `/api/projects/{projectId}/metrics` — Ingest metrics (bulk, rôle `ADMIN`/`DEVELOPER`). Chaque point déclenche immédiatement l'évaluation des `AlertRule` du projet pour cette métrique.
  - Body: `{ "points": [{ "serviceName": "..." (optionnel), "metricName": "cpu_usage_percent", "value": 87.5, "unit": "%" (optionnel), "capturedAt": "..." (optionnel) }] }`
- GET `/api/projects/{projectId}/metrics?metric=...&from=...&to=...` — Query metrics. Sans `metric`, retourne tout l'historique du projet (le plus récent d'abord). Avec `metric`, filtre sur `from`/`to` (par défaut : dernières 24h), tri chronologique.

## Logs (ingest & search) ✅ implémenté et testé (voir `LogController`, `ObservabilityIntegrationTest`)
- POST `/api/projects/{projectId}/logs` — Ingest logs (bulk, rôle `ADMIN`/`DEVELOPER`)
  - Body: `{ "entries": [{ "serviceName": "...", "environment": "...", "level": "ERROR", "message": "...", "timestamp": "..." (optionnel) }] }`
- GET `/api/projects/{projectId}/logs?service=&environment=&level=&q=&from=&to=&page=&size=` — Search logs (pagination Spring standard : réponse `Page` avec `content`, `totalElements`, etc.)

## Incidents ✅ implémenté et testé (voir `IncidentController`, `IncidentServiceTest`)
- GET `/api/incidents?projectId=...` — List incidents for a project (Auth: Bearer requis, doit posséder le projet ou ADMIN)
- POST `/api/incidents` — Create incident manually (rôle `ADMIN` ou `DEVELOPER`). Statut initial toujours `OPEN`.
  - Body: `{ "projectId": ..., "serviceId": ... (optionnel), "serviceName": "..." (optionnel, si pas de serviceId), "title": "...", "description": "...", "severity": "LOW|MEDIUM|HIGH|CRITICAL" }`
- GET `/api/incidents/{id}` — ❌ pas encore implémenté (seule la liste filtrée par projet existe)
- PATCH `/api/incidents/{id}` — Update status / root cause / recommendations (rôle `ADMIN` ou `DEVELOPER`)
  - Body: `{ "status": "OPEN|INVESTIGATING|RESOLVED", "rootCause": "...", "recommendations": "...", "confidenceScore": 0.87 }`
  - `resolvedAt` est renseigné automatiquement au premier passage à `RESOLVED`.
  - Création automatique par le pipeline IA : ❌ pas encore branchée (l'`AiAnalysisService` existant appelle un service Python qui n'existe pas encore).

## Alerts & Alert Rules ✅ implémenté et testé (voir `AlertRuleController`, `AlertController`, `AlertEvaluationServiceTest`)
- POST `/api/projects/{projectId}/alert-rules` — Create rule (rôle `ADMIN`/`DEVELOPER`)
  - Body: `{ "name": "High CPU", "metric": "cpu_usage_percent", "operator": ">|<|>=|<=|==", "threshold": 80.0, "duration": "5m" (optionnel, stocké mais **pas encore évalué** — voir limitation ci-dessous), "severity": "LOW|MEDIUM|HIGH|CRITICAL", "enabled": true }`
- GET `/api/projects/{projectId}/alert-rules` — List rules for a project
- GET `/api/alerts?projectId=...` — List alerts for a project, most recent first
- POST `/api/alerts/{id}/ack` — Acknowledge alert (rôle `ADMIN`/`DEVELOPER`)
  - ⚠️ **Limitation connue** : l'évaluation se fait à chaque point de métrique ingéré, en comparant immédiatement à `threshold` — le champ `duration` (ex. "5m", condition qui doit tenir sur une fenêtre de temps) n'est **pas encore évalué**, une seule valeur qui dépasse le seuil ouvre l'alerte. Un vrai moteur à fenêtre glissante reste à faire.

## Deployments ✅ implémenté et testé (voir `DeploymentController`, `DeploymentServiceTest`)
- POST `/api/deployments` — Record a deployment event (rôle `ADMIN` ou `DEVELOPER`, doit posséder le projet). `author` est déduit automatiquement du token, pas envoyé par le client.
  - Body: `{ "projectId": ..., "version": "v1.0.0", "commit": "..." (optionnel), "branch": "..." (optionnel), "environment": "development|staging|production", "status": "SUCCESS|FAILED|IN_PROGRESS", "startedAt": "..." (optionnel), "finishedAt": "..." (optionnel) }`
- GET `/api/deployments?projectId=...` — List deployments for a project, most recent first

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

## Chaos Engineering ✅ implémenté et testé (voir `ChaosController`, `ChaosServiceTest`)
- POST `/api/projects/{projectId}/chaos` — Trigger a simulation (rôle `ADMIN`/`DEVELOPER` uniquement — `VIEWER` reçoit 403)
  - Body: `{ "action": "KILL_POD|CPU_LOAD|LATENCY|HTTP_500|DB_FAILURE", "targetService": "..." (optionnel) }`
  - ⚠️ **Simulation uniquement** : aucune infrastructure Kubernetes réelle n'est affectée (Phase 7 pas encore faite). L'appel crée un `Incident` (sévérité selon l'action) pour démontrer le flux détection→investigation→résolution.
  - Refusé par défaut sur un projet `environment=production` (message explicite), sauf `CHAOS_ALLOW_PRODUCTION=true`.
- GET `/api/projects/{projectId}/chaos` — Historique des simulations pour un projet

## Demo Mode ✅ implémenté (voir `DemoDataSeeder`)
- `DEMO_MODE=true` (variable d'environnement) : au démarrage, crée un utilisateur `demo@devpulse.local` / `DemoPass123!` (identifiants de démo, jamais un vrai secret), un projet avec 3 services, 2 incidents, 3 déploiements, une règle d'alerte + une alerte réellement déclenchée via `AlertEvaluationService`. Idempotent : ne recrée rien si l'utilisateur demo existe déjà.

## Users ❌ pas encore implémenté
- Pas de `/api/users` (gestion des comptes par un ADMIN) pour l'instant.

> Note: This is an overview. I will generate a full OpenAPI YAML from controllers (or hand-author `docs/openapi.yaml`) next.