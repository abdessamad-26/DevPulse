# DevPulse release checklist

## Verified locally

- [x] Java 25 LTS is configured in the Maven build, backend container stages,
  CI JDK, and README.
- [x] Backend baseline before the account/admin follow-up, on Temurin JDK
  25.0.4.1: **94 tests passed, 0 failures/errors**.
- [x] Logs page exposes loading, empty, populated, and error states; retry
  behavior has regression coverage.
- [x] Frontend `npm test -- --watch=false`: **8 tests passed in 4 files**.
- [x] Frontend `npm run build`: **passed**.
- [x] `npm audit --audit-level=low`: **0 vulnerabilities**.
- [x] Final GitHub Security Advisories scan of the previously affected
  dependency coordinates: **no known vulnerabilities found**.
- [x] Python baseline `python -m pytest`: **22 passed**, with 25 dependency
  deprecation/compatibility warnings. Python was not changed during this
  remediation.
- [x] `docker compose config --quiet`: **passed**.
- [x] `git diff --check`: **passed**.
- [x] Dashboard and metrics pages distinguish API failures from successful empty results and provide retry actions.
- [x] Frontend after these changes: `npm test -- --watch=false` — **8 tests passed**; `npm run build` — **passed**.

## Implemented and backend verified

- Self-service profile/password APIs and admin account controls are implemented
  with server-side authorization, session revocation, audit events, and
  safeguards for the current and last active administrator.
- Integration coverage for these API flows now passes in `mvn clean verify`: **92 tests passed, 0 failures/errors** on Oracle JDK 26.0.1 (Java release target 25).

## Required before a production release

- [ ] Run Docker image builds and verify container startup; the local Docker
  daemon was unavailable during this work.
- [ ] Run Flyway against a fresh PostgreSQL database and an existing database,
  then verify schema validation and restart behavior.
- [ ] Run the full CI workflow in GitHub Actions and review all required jobs,
  including image scans.
- [ ] Add/run end-to-end tests for critical user journeys and the Java-Python
  service contract; no browser E2E suite was verified here.
- [x] Run backend `mvn clean verify` for the account/admin APIs and added integration tests: **92 tests passed, 0 failures/errors**; build and package verification succeeded.
- [ ] Add an invitation flow if the product needs inviting people who do not yet have accounts.
- [ ] Decide whether Prometheus/Grafana/Loki deployment is required or update
  architecture expectations; these services are not present in Compose.
- [ ] Re-run Python checks in the chosen supported runtime and resolve or
  explicitly accept the existing dependency deprecation/compatibility warnings.
- [ ] Review deployment secrets, database backup/restore, rollback, and
  environment-specific configuration in the target environment.
- [ ] Obtain an independent security review and validate authorization against
  a running deployment; dependency scanning alone is not a penetration test.

## Release verdict

**Not ready for production yet.** Backend verification now passes, including
the new account/admin APIs; the frontend tests/build also pass.
Docker/PostgreSQL/Flyway behavior, full-system E2E, target-environment CI,
monitoring deployment, and some product workflows remain unverified or
incomplete. Do not deploy until the unchecked release-blocking items above are
resolved and reviewed.
