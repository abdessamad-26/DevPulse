# DevPulse Roadmap & Milestones

This roadmap breaks down the project into milestones and deliverables with rough estimates for an MVP.

## Phase 0 — Analysis & Design (1 week)
- Deliverables: Vision, Architecture docs, Data model, API list, K8s plan, CI plan, Security plan.

## Phase 1 — Repository & Tooling (1 week)
- Initialize monorepo structure
- Add `.github/workflows/ci.yml` skeleton
- Add `.env.example`, `Makefile` or `scripts/` helpers
- Basic README and contributing guide

## Phase 2 — Backend Core (3–4 weeks)
- Spring Boot skeleton
- Entities, repositories, DTOs, mappers
- Authentication (JWT + refresh), roles
- Flyway migrations and SQL schema
- Unit tests and integration tests for repositories

## Phase 3 — Frontend (3–4 weeks)
- Angular shell and auth flow
- Dashboard widgets (KPI cards)
- Projects and incidents pages
- Charts using Chart.js
- Responsive layout and accessibility

## Phase 4 — Observability & AI (3 weeks)
- Instrumentation (OpenTelemetry)
- Prometheus/Loki/Grafana setup + dashboards
- AI Service basic rule engine and statistical detectors
- Correlation of deployments → incidents

## Phase 5 — Docker & Compose (1 week)
- Dockerfiles for frontend/backend/ai-service
- `docker-compose.yml` for local demo (Postgres, prometheus, grafana, loki)
- Demo seed data and demo mode

## Phase 6 — Kubernetes & GitOps (2–3 weeks)
- K8s manifests for all services
- ArgoCD application manifests and sync settings
- Health checks, resource quotas, PVCs

## Phase 7 — CI/CD Harden & Security (2 weeks)
- Full CI pipeline: lint, test, build, scan, docker, push
- Container scanning (Trivy), dependency scanning
- Secrets integration guidance (K8s secrets / Vault)

## Phase 8 — Testing, Docs, Demo (2 weeks)
- E2E tests and coverage targets
- Final docs, demo video assets, screenshots
- Release tagging and CHANGELOG

## Estimates & Team
- MVP estimate: 12–16 weeks for a small team (1–3 engineers)
- Single-engineer timeline will be longer; use iterative delivery and demos to validate.

## Prioritization
1. Backend core + Auth + DB migrations
2. Frontend skeleton + Auth flow
3. Observability ingestion + AI basic
4. Local Docker Compose demo
5. K8s + ArgoCD
6. CI/CD and security hardening

## Acceptance criteria per milestone
Each milestone must include:
- code compiled and unit-tested
- integration test for key flows
- documentation updated
- commit history with meaningful messages
- demo steps to reproduce locally


---

For the next sprint I will scaffold Phase 1 files and CI skeleton. If you confirm, I will create a minimal `.github/workflows/ci.yml`, `.env.example`, and `docs/README.md` improvements, then commit.