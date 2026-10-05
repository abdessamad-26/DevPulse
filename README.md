# DevPulse

DevPulse is a local-first observability and incident-management application. It combines an Angular web UI, a Spring Boot API backed by PostgreSQL, and a small FastAPI service for incident analysis and statistical anomaly detection.

## Current scope

- User registration and login with JWT access and refresh tokens.
- Project-scoped services, incidents, deployments, alerts, metrics, and logs.
- Deterministic incident analysis and z-score anomaly detection.
- A demo account and seed data, enabled only when `DEMO_MODE=true`.
- Docker Compose development stack with PostgreSQL, backend, AI service, and frontend.

The project is under active development. Project membership, refresh-token rotation/revocation, audit-log APIs, time-window alert evaluation, live infrastructure chaos experiments, and production monitoring dashboards are not implemented yet. See [docs/api.md](docs/api.md) for the current API and its limitations.

## Architecture

| Component | Technology | Local address |
| --- | --- | --- |
| Frontend | Angular | http://localhost:3000 |
| Backend API | Java 21, Spring Boot, Spring Security, JPA, Flyway | http://localhost:8080 |
| AI service | Python 3.11, FastAPI | http://localhost:8000 |
| Database | PostgreSQL 15 | localhost:5432 (bound to loopback) |

The main source directories are `frontend/`, `backend/`, and `ai-service/`. Compose and local-development scripts are at the repository root and in `scripts/`.

## Run locally with Docker

Prerequisites: Docker Desktop with the Compose plugin.

On Windows PowerShell:

```powershell
.\scripts\dev.ps1
```

The script creates a git-ignored `.env` with a random JWT signing secret on first use, enables demo mode for that local setup, then builds and starts the Compose services. It does not overwrite an existing `.env`.

On macOS or Linux:

```sh
cp .env.example .env
```

Edit `.env` and set a strong, unique `DB_PASSWORD` and `JWT_SECRET` (at least 32 characters), then run:

```sh
docker compose up -d --build
docker compose ps
```

Compose uses the Spring `prod` profile with PostgreSQL and Flyway migrations by default. Override `SPRING_PROFILES_ACTIVE` only when you intentionally want another profile. Health status is visible with `docker compose ps`.

When demo mode is enabled, use `demo@devpulse.local` / `DemoPass123!`. These are public demo credentials: do not enable demo mode on an internet-accessible deployment.

To stop the services without deleting database data:

```sh
docker compose down
```

`docker compose down -v` also deletes the PostgreSQL volume and all local database data.

## Run checks

Backend:

```sh
cd backend
mvn -B clean test
```

Frontend (Node.js 24.15.0 or later supported by Angular CLI 22):

```sh
cd frontend
npm ci
npm test -- --watch=false
npm run build
```

AI service:

```sh
cd ai-service
python -m pip install -r requirements-dev.txt
python -m pytest
```

The GitHub Actions workflow runs these checks, builds the container images, and scans the images for fixable HIGH and CRITICAL vulnerabilities with Trivy.

## Configuration and security

- Start from [.env.example](.env.example); keep `.env` private and never commit it.
- Generate a strong JWT signing secret. The Windows startup script does this automatically when it creates `.env`.
- Use `DEMO_MODE=false` outside a local demo environment.
- The `prod` Spring profile requires PostgreSQL connection settings, runs and validates Flyway migrations, and uses Hibernate schema validation instead of schema updates.
- Actuator exposes health, info, and metrics endpoints. Do not expose management endpoints publicly without an appropriate access and network policy.

## Documentation

- [docs/api.md](docs/api.md): implemented HTTP endpoints, request examples, authorization, and known API limitations.
- [docs/architecture.md](docs/architecture.md): architecture and design notes.
- [CONTRIBUTING.md](CONTRIBUTING.md): contribution guidance.

## License

DevPulse is licensed under the Apache License 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
