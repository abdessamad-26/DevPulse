# Operations review — November 2026

This is a focused review of the operational changes and remaining deployment risks. It is not a production-readiness certification.

## Implemented in this branch

- Spring Boot Actuator Prometheus registry and scrape endpoint.
- Prometheus scrape configuration for the backend.
- Grafana datasource provisioning and a first dashboard for request rate, 5xx rate, p95 latency, JVM heap and uptime.
- Compose health checks and a startup command that waits for healthy services.
- Browser E2E is already part of CI: it starts an isolated Compose stack, runs the Playwright demo flow against the live frontend, captures service logs on failure and removes the stack.
- Kubernetes starter manifests for frontend, backend, FastAPI and PostgreSQL, plus a Flux GitRepository/Kustomization example.
- Local Grafana credentials are generated separately from the JWT secret by the first-run helper scripts.

## Security review and remaining work

### Actuator and Prometheus

The Prometheus scrape endpoint is unauthenticated so Prometheus can scrape it without application credentials. It must remain private to the Compose network or Kubernetes cluster network. Compose publishes only loopback-bound ports. In Kubernetes, add a NetworkPolicy that allows scrape access only from the Prometheus namespace/workload before deploying a Prometheus instance; do not publish the backend actuator endpoint through an Ingress.

### Credentials

- Keep the real .env out of Git and use unique database, JWT and Grafana credentials.
- The Kubernetes manifests intentionally reference a pre-created devpulse-secrets Secret. Use External Secrets, Sealed Secrets or SOPS for cluster operations.
- Demo mode is disabled in the Kubernetes ConfigMap. Never deploy the public demo credentials to an internet-accessible environment.
- Rotate any credentials that have ever been committed or shared.

### Kubernetes limitations

- The image names under ghcr.io/abdessamad-26 are placeholders until a separate image-publishing workflow is configured and the packages exist.
- The Flux example tracks modernize/p1-quality for review. Change it to the release branch after merge and before enabling reconciliation.
- PostgreSQL is a single replica with a PVC; configure backups and prove restore before relying on it.
- No Ingress/TLS, NetworkPolicy, HPA, PodDisruptionBudget, resource autoscaling policy or secret-controller integration is included.
- The frontend CORS origin is an example domain and must be replaced with the actual HTTPS origin.
- Kubernetes monitoring is not provisioned by these manifests; the first Grafana dashboard is currently for Docker Compose.

### Performance and reliability

- The dashboard provides an initial baseline, not SLOs. Establish request-rate, error-rate and latency objectives from observed traffic.
- Add alert rules for sustained 5xx rate, latency, JVM memory pressure, database connectivity and scrape target availability.
- Load-test API and ingestion endpoints before increasing replicas or CPU/memory limits.
- Review connection-pool sizing, pagination limits, retention and database indexes using representative data.
- Add backup/restore drills, graceful shutdown checks and dependency-failure tests.

## Local verification procedure

On Windows, run scripts/dev.ps1 after reviewing the generated .env. On macOS/Linux, create .env from .env.example, set DB_PASSWORD, JWT_SECRET and GRAFANA_ADMIN_PASSWORD, then run:

    docker compose up --build -d --wait --wait-timeout 300
    docker compose ps
    curl -fsS http://localhost:8080/actuator/health
    curl -fsS http://localhost:8080/actuator/prometheus
    curl -fsS http://localhost:9090/-/healthy
    curl -fsS http://localhost:3001/api/health

Run the browser flow from frontend with Playwright Chromium installed:

    npm ci
    npx playwright install chromium
    npm run test:e2e

The CI workflow executes the same browser flow against a disposable live Compose stack. A successful GitHub Actions run is the runtime evidence for that commit; GitHub file edits alone are not evidence that Docker Desktop or a local browser stack was started here.
