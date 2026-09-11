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
├── infrastructure/
│   ├── docker/
│   ├── kubernetes/
│   └── argocd/
├── monitoring/
│   ├── prometheus/
│   ├── grafana/
│   └── loki/
├── docs/
├── scripts/
├── tests/
├── .github/
│   └── workflows/
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

## Documentation

- [docs/architecture.md](docs/architecture.md) — system architecture, domain model, API plan, Kubernetes and CI/CD strategy
- [CONTRIBUTING.md](CONTRIBUTING.md) — contribution policy and branching model
- [.env.example](.env.example) — environment template without secrets

## License

This project is currently in the foundation and architecture stage. Licensing will be finalized once the first functional release is ready.

## Next milestone

The next steps will focus on backend foundation, authentication, project management, and the first working API contracts before UI and deployment layers are implemented.
