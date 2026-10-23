# Changelog

Notable changes to DevPulse are recorded here. This project has not published a versioned release yet.

## Unreleased

### Added

- Upgraded the backend runtime, CI JDK, and container images to Java 25 LTS.
- Updated affected Angular, MCP SDK, Vitest, `source-map-js`, and Vite packages
  to patched compatible releases; the final npm audit reports no known
  vulnerabilities.
- Added distinct loading and request-error states with retry to the Logs page.
- Added development and production Spring profiles. The production profile uses PostgreSQL, Flyway migration validation, and Hibernate schema validation.
- Added Docker Compose healthchecks for PostgreSQL, the backend, the AI service, and the frontend, with startup ordering for dependent services.
- Expanded CI to run backend, frontend, and AI-service tests, cache language dependencies, build all container images, and scan them with Trivy.
- Rewrote the project overview and API reference to describe the current implementation and known limitations.

### Fixed

- Prevented failed log searches from being presented as successful empty results.
- Return a JSON-compatible `null` z-score for an anomalous value compared with constant history instead of serializing infinity.
