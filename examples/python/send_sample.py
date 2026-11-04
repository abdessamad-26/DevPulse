"""Send one metric and one log to a DevPulse project using environment config."""

from __future__ import annotations

import os
import sys

from devpulse_client import DevPulseClient


def required_env(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        raise ValueError(f"Set the {name} environment variable.")
    return value


def main() -> int:
    try:
        client = DevPulseClient(
            api_url=os.environ.get("DEVPULSE_API_URL", "http://localhost:8080"),
            project_id=int(required_env("DEVPULSE_PROJECT_ID")),
            ingestion_key=required_env("DEVPULSE_INGESTION_KEY"),
            service_name=os.environ.get("DEVPULSE_SERVICE_NAME", "sample-checkout-api"),
            environment=os.environ.get("DEVPULSE_ENVIRONMENT", "development"),
        )
        metric_status = client.ingest_metric("request_duration_ms", 142.8, "ms")
        log_status = client.ingest_log("INFO", "Sample checkout request completed successfully.")
    except (ValueError, RuntimeError) as error:
        print(f"DevPulse sample failed: {error}", file=sys.stderr)
        return 1

    print(f"Metric accepted (HTTP {metric_status}); log accepted (HTTP {log_status}).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
