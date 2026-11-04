# DevPulse demo walkthrough

This walkthrough shows how DevPulse connects a metric threshold to an alert and
an incident investigation. It uses synthetic data in a local Docker Compose
environment.

## Start the app

Start Docker Desktop, then run the Windows startup script:

```powershell
.\scripts\dev.ps1
```

Open [http://localhost:3000](http://localhost:3000) and sign in with
`demo@devpulse.local` / `DemoPass123!`. Select the **DevPulse Demo** project if
it is not already selected.

## Trigger an alert

1. Open **Alerts** and choose **New rule**.
2. Create a rule named `High checkout latency` for metric
   `checkout_latency_ms`, operator `>`, threshold `80`, and severity `HIGH`.
3. Open **Metrics**, choose **Ingest metric**, and send metric
   `checkout_latency_ms` with value `95`, service `checkout-api`, and unit `ms`.
4. Return to **Alerts**. The new rule evaluates each ingested point
   immediately, so the value `95` opens a `HIGH` alert.

This shows what the alert rule is for: turn a measured condition into a visible,
project-scoped signal for the team.

## Investigate an incident

1. Open **Incidents** and choose **Report incident**.
2. Use title `Checkout requests failing` and describe the symptom, for example
   `Checkout requests are timing out while the database connection pool is exhausted.`
3. Create the incident and choose **Analyze** on its row.
4. DevPulse displays a classification, confidence, likely root cause,
   recommendations, and the signals used by its analysis service.

The current analyzer uses deterministic rules and the incident text; it does
not call a hosted generative AI model. Teams can update the incident status and
review related deployments from the incident page.

## Send a sample from a service

To see how an application can send its own telemetry, create a project-scoped
ingestion key from **Access**, then follow
[docs/integrations.md](integrations.md) to run the Python reference sender. The
sample sends one metric and one log entry. The key can ingest data only for its
own project.

## Automated browser walkthrough

With the local Docker stack running, execute the Playwright flow from
`frontend/`:

```sh
npx playwright install chromium
npm run test:e2e
```

The test creates uniquely named synthetic records in the selected project. CI
runs it against a fresh disposable database and removes that stack afterward.

## Current limits of this demo

- Alert rules evaluate each metric point immediately; rolling time windows are
  not currently part of this rule flow.
- The Python sender is a small REST example, not an OpenTelemetry collector,
  Prometheus scraper, or automatic instrumentation agent.
- Demo credentials are public and intended only for local use. Do not enable
  demo mode on an internet-accessible deployment.
