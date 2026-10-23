# DevPulse initial audit

**Audit date:** 2026-10-23
**Scope:** repository state at the start of this work, including the pre-existing
working-tree changes. No commit, push, deployment, or production operation was
performed.

## Executive summary

The repository contains a real Angular application, a Spring Boot API, a
FastAPI analysis service, PostgreSQL/Flyway migrations, and a CI workflow. The
initial frontend and Python checks passed and Compose configuration is
syntactically valid. Subsequent remediation verified the Java 25 backend build
and all 94 Java tests, and the frontend now passes all 8 tests and a production
build. Docker-backed PostgreSQL/Flyway and container-image verification remain
blocked because the local Docker daemon is unavailable.

The initial working tree was not clean: 40 tracked files were modified, with
additional untracked files and directories. Those changes are treated as user
work and are preserved. In particular, `auth_cookies.txt` was not opened or
modified.

No missing project or incident detail endpoint was found: the backend already
has `GET /api/projects/{id}` and `GET /api/incidents/{id}`. Alert-rule durations
are also represented and evaluated in the backend; the API documentation
describes sustained-breach evaluation. The README's statement that duration
evaluation is not implemented is stale and is corrected as part of this work.

At audit start, the backend build, Dockerfile, and CI workflow targeted Java 21.
The earlier request to move to the current LTS is included in this remediation:
Java 25 is the latest LTS release as of the audit date, according to the
[Oracle Java SE support roadmap](https://www.oracle.com/java/technologies/java-se-support-roadmap.html).

## Priority findings

### P1 — PostgreSQL and container verification are blocked locally

- **Evidence:** `backend/pom.xml` is the Maven build definition, but there is no
  `backend/mvnw.cmd`; initially Maven was absent from `PATH`. A verified
  temporary Maven 3.9.15 and Temurin JDK 25.0.4.1 were subsequently used to run
  `mvn clean verify` successfully: 94 tests passed, with no failures or errors.
- **Evidence:** Docker image build could not connect to the Docker Desktop
  daemon. Consequently PostgreSQL-backed startup, fresh/existing Flyway
  migrations, and container image execution were not tested.
- **Risk:** Java compile/test status is verified, but production database and
  container behavior remain unverified. This remains a release-verification
  blocker, not evidence of a Java code failure.
- **Acceptance:** Run the image and fresh/existing PostgreSQL migration checks
  in an environment with a working Docker/container runtime.

### P2 — Logs request failures were reported as an empty result (resolved)

- **Evidence:** `frontend/src/app/pages/logs/logs.component.ts` sets its
  `loaded` flag on both success and error, but does not retain or display an
  error state. A failed request therefore renders the same “No log entries
  match” message as a successful empty response.
- **Risk:** Operators could mistake an API outage or authorization failure for
  an absence of operational data.
- **Resolution:** The page now distinguishes loading, successful empty,
  populated, and error states, and provides retry. Its regression test verifies
  that an error is shown and retry recovers. Frontend tests and production
  build pass.

### P2 — User administration and self-service settings are not implemented

- **Evidence:** `frontend/src/app/app.routes.ts` has an `access` route for
  project access, but no user-administration route. The backend controller
  inventory contains no user-management controller. The Settings page states
  that administration and self-service password changes are not implemented.
- **Risk:** Global user administration and account settings cannot be
  completed through the product UI/API. Adding UI-only controls would be
  misleading and unsafe.
- **Acceptance:** Define the required user lifecycle/API contract and server
  authorization rules before implementing these workflows.

### P2 — Monitoring deployment is not present in the repository

- **Evidence:** `docker-compose.yml` defines PostgreSQL, backend, AI service,
  and frontend, but no Prometheus, Grafana, or Loki services. The
  `infrastructure/`, `monitoring/`, and `tests/` directories contain no tracked
  files at audit time.
- **Risk:** The architecture document describes monitoring integrations that
  cannot be verified or deployed from the checked-in configuration.
- **Acceptance:** Either add and validate the intended monitoring/deployment
  configuration or clearly label those architecture elements as future work.

### P2 — PostgreSQL-specific migration behavior is not locally verified

- **Evidence:** Production configuration enables Flyway and Hibernate schema
  validation, while the default local configuration uses in-memory H2 and
  disables Flyway. The Java Maven suite now passes, but the local Docker daemon
  is unavailable, preventing PostgreSQL-backed validation.
- **Risk:** Migration compatibility on a fresh and an existing PostgreSQL
  database remains unverified.
- **Acceptance:** Exercise Flyway against both database states using a
  PostgreSQL-backed integration test or a repeatable local verification job.

### P3 — Python baseline emits dependency deprecation warnings

- **Evidence:** `python -m pytest` passes, but the run reports deprecation
  warnings from Starlette/FastAPI integration and Python 3.14 compatibility
  warnings from the installed dependency set.
- **Risk:** Future dependency/runtime upgrades may turn these warnings into
  failures.
- **Acceptance:** Confirm supported Python versions and resolve warnings
  through compatible dependency upgrades, without weakening tests.

## Baseline evidence

| Component | Command/result |
| --- | --- |
| Git | Branch `modernize/p1-quality`; 40 tracked paths modified before this work; untracked files were present. |
| Backend baseline and remediation | Initial `mvn -B clean test` could not start: Maven not installed/on `PATH`; wrapper absent. With checksum-verified temporary Maven 3.9.15 and Temurin JDK 25.0.4.1, `mvn clean verify` — **94 tests passed, 0 failures/errors**. |
| Frontend tests | Initial `npm test -- --watch=false` — **7 passed, 3 test files**. After the Logs regression test and dependency remediation, same command — **8 passed, 4 test files**. |
| Frontend build | `npm run build` — **passed** initially and after dependency remediation. |
| AI service | `python -m pytest` — **22 passed**, with 25 dependency deprecation/compatibility warnings. |
| Compose | `docker compose config --quiet` — **passed**. |
| Backend image | `docker build --tag devpulse-backend-baseline:local .` — **not run to completion**; Docker daemon unavailable. |
| Security review | The read-only review of the pre-existing changes returned no findings. This is not a complete penetration test or a substitute for CI/dependency scanning. |
| Frontend dependency scan | Initial scan found 14 advisories across Angular Router, MCP server, Vitest mocker, `source-map-js`, and Vite. Patched compatible releases were installed; `npm audit --audit-level=low` and the final advisory scan of affected coordinates both report **0 known vulnerabilities**. |

## Verified capabilities and limitations

- Backend route mappings include project and incident detail reads, paginated
  APIs, project membership, ingestion keys, audit logs, alert rules, and
  refresh/logout endpoints.
- Alert rules have a duration field and the metric ingestion path invokes
  duration-aware evaluation. Unit tests exist for alert evaluation and
  duration parsing.
- The AI service exposes health/readiness, incident analysis, and anomaly
  routes. Its test suite covers API behavior, analysis, and anomaly detection.
- The CI workflow builds/tests Java, Angular, and Python and scans three
  container images for fixable HIGH/CRITICAL vulnerabilities. Dependabot is
  configured for Maven, npm, pip, Docker, and GitHub Actions.
- The frontend has only three test files (seven tests at baseline); there are
  no checked-in browser E2E tests.
- The backend has 21 Java test files. PostgreSQL-specific integration and
  migration verification remain unproven by the local baseline.
- Java 25 LTS was selected for the requested runtime upgrade; POM, backend
  build/runtime image, CI, and README now target 25. Compilation and all Java
  tests passed on the target runtime; container and PostgreSQL checks remain
  blocked by the Docker daemon.
- The frontend dependency lockfile now resolves Angular 22.2.3, Angular
  Build/CLI 22.2.4, and Vite 8.3.5 (including the Angular Build override).
  `npm audit` reports no known vulnerabilities. This does not replace a full
  application security assessment.
- The license file and NOTICE are present; README identifies Apache License
  2.0.

## Scope and constraints

This audit does not claim that an external service, deployment, production
environment, or unavailable local build succeeded. Existing uncommitted user
changes have been preserved. No secret-bearing local files were inspected.
