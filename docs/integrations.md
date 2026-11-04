# Sending telemetry to DevPulse

DevPulse accepts project-scoped metric and log batches through its REST API.
The reference Python client in `examples/python/` uses only the Python standard
library, so a small service can send its own telemetry without another runtime
agent.

## Create an ingestion key

1. Sign in and open **Access** for the project.
2. Create an ingestion key and copy it immediately. DevPulse stores only a
   hash; the secret is shown once.
3. Keep the key in your secret manager or environment configuration. Do not
   place it in source control.

The key can only ingest metrics and logs for its own project. It cannot read
project data or call other APIs. Send it in the `X-API-Key` header.

## Run the Python example

Python 3.11 or later is required. The example uses only the standard library.
For the local Docker demo, get the project ID from the **Projects** page and
set the key you just created:

```powershell
$env:DEVPULSE_API_URL = "http://localhost:8080"
$env:DEVPULSE_PROJECT_ID = "1"
$env:DEVPULSE_INGESTION_KEY = "<paste-the-key-here>"
$env:DEVPULSE_SERVICE_NAME = "checkout-api"
$env:DEVPULSE_ENVIRONMENT = "development"
python .\examples\python\send_sample.py
```

The script sends one `request_duration_ms` metric point and one `INFO` log
entry. Open **Metrics** and **Logs** in the selected project to see them. If a
matching alert rule exists, metric ingestion also runs the project's alert
evaluation.

For a remote DevPulse server, set `DEVPULSE_API_URL` to an HTTPS URL. The client
refuses to send the ingestion key to a remote HTTP host. Timestamps are sent as
UTC `LocalDateTime` values, matching the current API contract.

## API limits

This is a reference Python sender for the existing REST ingestion API; it is
not an OpenTelemetry exporter or an auto-discovery agent. OpenTelemetry,
Prometheus, and Loki integrations remain future work. See [the API reference](api.md)
for payload limits and authentication behavior.
