# DevPulse

## AI-Powered DevOps & Application Observability Platform

DevPulse is a professional observability and incident management platform designed for modern software teams. It brings together application monitoring, deployment tracking, AI-driven incident analysis, alerting, and operational visibility in a single product experience.

## Current status

This repository is currently being structured following the phased roadmap described in the project brief. The core architecture, responsibilities, and repository blueprint are defined first so that later service implementations remain coherent and production-ready.

## Why DevPulse?

Teams often operate with fragmented tools: metrics in one place, logs in another, incidents tracked separately, and deployment history buried in CI pipelines. DevPulse addresses this by consolidating these signals into a single platform with a clear operational workflow:

- connect projects and services
- monitor health and performance
- detect anomalies early
- correlate incidents with deployment history
- analyze root cause with AI-assisted reasoning
- recommend remediation actions
- support chaos simulation in safe environments

## Architecture summary

DevPulse follows a modular monorepo structure:

- frontend: Angular + PrimeNG dashboard and operational UI
- backend: Java + Spring Boot + PostgreSQL + Flyway + JWT-based security
- ai-service: Python + FastAPI for anomaly detection and incident analysis
- infrastructure: Docker, Kubernetes, ArgoCD and environment configs
- monitoring: Prometheus, Grafana and Loki
- docs: architecture, security, interview preparation and deployment docs

## Repository structure

```text
devpulse/
├── frontend/
├── backend/
├── ai-service/
DevPulse is a professional observability and incident management platform designed for modern software teams. It brings together application monitoring, deployment tracking, AI-driven incident analysis, alerting, and operational visibility in a single product experience.
│   ├── docker/
│   ├── kubernetes/
│   └── argocd/
The repository is currently in foundation stage. The frontend command center is now runnable and monitors the backend health endpoint; the domain API and AI analysis workflows are still being implemented.
│   ├── prometheus/
│   ├── grafana/
│   └── loki/
├── docs/
├── scripts/
├── tests/
├── .github/
│   └── workflows/
- ✅ runnable frontend command center added

### Current implementation status
- Frontend: ✅ **real Angular app, production build verified** (`npm install && ng build` — 1.08 MB, 0 errors) — Angular 19 + PrimeNG + Reactive Forms. Auth (login/register/guard/interceptor), layout with navigation, and Dashboard/Incidents pages are wired to the real backend; Logs/Metrics/Deployments/Alerts/Settings are honest placeholders (backend APIs already exist for them, Angular pages don't yet). No `.spec.ts` tests written yet. Old static HTML/JS prototype moved to `frontend/legacy-static-prototype/` (unused by the build).
- Backend: Spring Boot foundation, persistence entities/services, security configuration, and health endpoints are present. **Authentication (`/api/auth/register|login|refresh|logout`) and project management (`/api/projects`) are now exposed through real HTTP controllers, protected by JWT + RBAC (`@PreAuthorize`), and covered by unit + integration tests** — see `docs/architecture.md#16` for exact scope and known limitations (no refresh-token rotation yet, no `/api/users` admin endpoints yet).
- AI service: ✅ **réellement installé et testé** (FastAPI, 16/16 tests pytest passent) — `IncidentAnalyzer` (règles déterministes) + `z_score_anomaly` (couche statistique). Contrat JSON avec le backend Java vérifié par test. Pas de couche LLM (optionnelle) pour l'instant ; pas de test d'intégration Java↔Python réel (les deux services n'ont jamais tourné ensemble ici).
- PostgreSQL: configured as a Compose dependency; runtime connectivity still needs an end-to-end environment check.
├── docker-compose.yml
├── .env.example
├── README.md
├── LICENSE
├── CONTRIBUTING.md
├── CHANGELOG.md
└── .gitignore
```

## Roadmap status

### Phase 0: Architecture and design
- ✅ vision and domain boundaries defined
- ✅ technical architecture documented
- ✅ repository structure bootstrapped
- ✅ risks, validation criteria and milestones defined

### Phase 1: Repository foundation
- ✅ project folders created
- ✅ initial documentation added
- ✅ CI workflow scaffolded
- ✅ environment template prepared

### Phase 2: Backend core
- ✅ Spring Boot skeleton, JPA entities (`User`, `Role`, `Project`, `ServiceEntity`, `Incident`, `Deployment`, `AlertRule`, `Alert`, `Metric`, `LogEntry`), migrations `V1`–`V4`
- ✅ centralized exception handling (`GlobalExceptionHandler` + `ApiException`, incl. 403 mapping for `AccessDeniedException`)
- ✅ Full project-scoped resource CRUD: services, incidents, deployments, alert rules, alerts, metrics (ingest + query), logs (ingest + filtered/paginated search) — all gated by `ProjectAccessService` ownership checks and covered by unit + integration tests
- ✅ **Real alert evaluation**: ingesting a metric immediately checks it against the project's enabled `AlertRule`s and opens an `Alert` if breached (`AlertEvaluationService`) — not a placeholder, see `AlertEvaluationServiceTest` and `ObservabilityIntegrationTest`
- ❌ entities/endpoints for audit_logs (table exists in SQL, no JPA mapping yet)
- ❌ `GET /api/projects/{id}`, `GET /api/incidents/{id}` (single-resource fetch) not implemented
- ❌ Swagger UI not yet verified end-to-end (dependency present, no manual check done)
- ⚠️ Alert evaluation known limitation: the `duration` field on a rule (time window) is stored but not evaluated — a single breaching data point triggers the alert immediately (see `docs/api.md`)

### Phase 3: Authentication
- ✅ register / login / refresh / logout endpoints (`AuthController`)
- ✅ real JWT issuance and validation on every request (`JwtService`, `JwtAuthenticationFilter`)
- ✅ RBAC enforced on `/api/projects` via `@PreAuthorize`
- ✅ role seeding at startup (`RoleSeeder`) so registration works out of the box
- ✅ unit tests (`AuthServiceTest`, `ProjectServiceTest`, `JwtServiceTest`) + integration test (`AuthFlowIntegrationTest`)
- ❌ no refresh-token rotation/blacklist
- ❌ no admin-only `/api/users` management endpoints yet
- ⚠️ **not yet verified to compile/pass on this machine** — see note below

### Phase 4: Frontend
- ✅ Angular 19 app scaffolded and **build-verified** (see Frontend section above)
- ✅ layout (sidebar + navigation), login/register, dashboard (projects), incidents
- ❌ logs, metrics, deployments, alerts, settings, users pages (placeholders only)
- ❌ no component/unit tests yet

### Phase 11: Chaos Engineering
- ✅ `POST/GET /api/projects/{id}/chaos` (`ChaosController`, `ChaosService`, `ChaosServiceTest`)
- ✅ disabled by default on `environment=production` projects (`CHAOS_ALLOW_PRODUCTION` override), VIEWER explicitly blocked
- ⚠️ **Honest scope**: simulations do not touch real infrastructure (no Kubernetes yet) — they record the event and open a matching `Incident` so the detection→resolution workflow can be demoed

### Phase 26/27: Demo Mode & Seed Data
- ✅ `DEMO_MODE=true` seeds a demo user, project, 3 services, 2 incidents, 3 deployments, an alert rule and a real triggered alert (`DemoDataSeeder`, `DemoDataSeederTest`) — idempotent, runs after `RoleSeeder`
- ✅ demo credentials are clearly fake and documented (`demo@devpulse.local` / `DemoPass123!`), never a real secret

## Documentation

- [docs/architecture.md](docs/architecture.md) — system architecture, domain model, API plan, Kubernetes and CI/CD strategy
- [CONTRIBUTING.md](CONTRIBUTING.md) — contribution policy and branching model
- [.env.example](.env.example) — environment template without secrets

## License

This project is currently in the foundation and architecture stage. Licensing will be finalized once the first functional release is ready.

## Next milestone

The next steps will focus on backend foundation, authentication, project management, and the first working API contracts before UI and deployment layers are implemented.
