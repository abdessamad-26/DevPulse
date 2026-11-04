"""Small standard-library client for DevPulse's project-scoped ingestion API."""

from __future__ import annotations

import json
from datetime import datetime, timezone
from urllib.error import HTTPError, URLError
from urllib.parse import urlparse
from urllib.request import Request, urlopen


class DevPulseClient:
    def __init__(
        self,
        api_url: str,
        project_id: int,
        ingestion_key: str,
        service_name: str,
        environment: str = "development",
        timeout_seconds: float = 10,
    ) -> None:
        self.api_url = api_url.rstrip("/")
        self.project_id = project_id
        self.ingestion_key = ingestion_key
        self.service_name = service_name
        self.environment = environment
        self.timeout_seconds = timeout_seconds

        parsed = urlparse(self.api_url)
        if parsed.scheme != "https" and parsed.hostname not in {"localhost", "127.0.0.1", "::1"}:
            raise ValueError("Use HTTPS when sending an ingestion key to a remote DevPulse server.")

    def ingest_metric(self, metric_name: str, value: float, unit: str = "") -> int:
        timestamp = self._utc_timestamp()
        return self._post(
            f"/api/projects/{self.project_id}/metrics",
            {
                "points": [
                    {
                        "serviceName": self.service_name,
                        "metricName": metric_name,
                        "value": value,
                        "unit": unit,
                        "capturedAt": timestamp,
                    }
                ]
            },
        )

    def ingest_log(self, level: str, message: str) -> int:
        return self._post(
            f"/api/projects/{self.project_id}/logs",
            {
                "entries": [
                    {
                        "serviceName": self.service_name,
                        "environment": self.environment,
                        "level": level.upper(),
                        "message": message,
                        "timestamp": self._utc_timestamp(),
                    }
                ]
            },
        )

    def _post(self, path: str, payload: dict[str, object]) -> int:
        request = Request(
            f"{self.api_url}{path}",
            data=json.dumps(payload).encode("utf-8"),
            headers={
                "Content-Type": "application/json",
                "X-API-Key": self.ingestion_key,
            },
            method="POST",
        )
        try:
            with urlopen(request, timeout=self.timeout_seconds) as response:
                return response.status
        except HTTPError as error:
            detail = error.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"DevPulse returned HTTP {error.code}: {detail}") from None
        except URLError as error:
            raise RuntimeError(f"Could not reach DevPulse: {error.reason}") from None

    @staticmethod
    def _utc_timestamp() -> str:
        # The API accepts LocalDateTime values. Encode UTC without a trailing Z/offset.
        return datetime.now(timezone.utc).replace(tzinfo=None).isoformat(timespec="seconds")
