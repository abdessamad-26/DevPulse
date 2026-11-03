# DevPulse remediation plan

This plan is ordered by risk and is grounded in the initial evidence in
[AUDIT.md](./AUDIT.md). Existing uncommitted work is user-owned and must be
preserved.

| Order | Work | Acceptance criteria | Status |
| --- | --- | --- | --- |
| 1 | Record the actual repository state and baseline | Audit lists observed features, gaps, exact command results, and verification blockers without claiming unavailable checks passed. | Complete |
| 2 | Correct the Logs request-state handling | Loading, populated, empty, and error states are distinct; retry works; a regression test verifies recovery. | Complete |
| 3 | Upgrade the Java runtime to the latest LTS | POM, compiler release, Docker build/runtime images, CI JDK, and public runtime docs target Java 25; verify using JDK 25. | Complete; `mvn clean verify` passed (94 tests) |
| 4 | Align user-facing docs with implemented behavior | README no longer claims alert durations are unsupported; API details continue to match current routes. | Complete |
| 5 | Run and fix the Java verification suite | Maven build and all backend tests pass; failures are fixed without suppressing tests. | Complete; temporary verified Maven/JDK used |
| 6 | Verify PostgreSQL and Flyway behavior | Fresh and existing PostgreSQL database startup/migrations are exercised; schema validation succeeds. | Blocked locally: Docker daemon unavailable |
| 7 | Remediate frontend dependency advisories | Patch affected dependencies within current major lines, rescan, and rerun frontend checks. | Complete; npm audit and advisory scan clean; 8 tests/build pass |
| 8 | Close the product/API gaps | Define a safe account/admin API and enforce its role and session rules on the server; keep invitations and unsupported workflows out of the UI. | Complete for profile, password, and account role/status administration; invitations remain out of scope |
| 9 | Complete cross-component validation and CI | Angular, Java, Python, API integration, and critical browser flows are tested; CI fails on required build/test/scan failures. | Partially verified; see audit |
| 10 | Finalize release evidence | Release checklist and final report list exact changes, commands, results, residual risks, and manual actions. | Complete; release remains blocked by unchecked production validations |

## Working rules

- Do not commit, push, deploy, publish, or delete user data.
- Preserve all pre-existing working-tree changes and do not inspect local secret
  files.
- Prefer minimal, test-backed fixes that use the existing architecture and API.
- Report environment blockers as blockers; do not replace unavailable tests
  with unverified claims.
