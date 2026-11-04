import json
import unittest
from unittest.mock import patch

from devpulse_client import DevPulseClient


class FakeResponse:
    status = 201

    def __enter__(self):
        return self

    def __exit__(self, *_args):
        return False


class DevPulseClientTests(unittest.TestCase):
    def setUp(self):
        self.client = DevPulseClient(
            "http://localhost:8080/",
            42,
            "dp_ing_test-secret",
            "checkout-api",
            "staging",
        )

    @patch("devpulse_client.urlopen", return_value=FakeResponse())
    def test_metric_uses_project_scoped_ingestion_key_and_expected_payload(self, urlopen):
        status = self.client.ingest_metric("request_duration_ms", 142.8, "ms")

        self.assertEqual(status, 201)
        request = urlopen.call_args.args[0]
        self.assertEqual(request.full_url, "http://localhost:8080/api/projects/42/metrics")
        self.assertEqual(request.get_header("X-api-key"), "dp_ing_test-secret")
        payload = json.loads(request.data)
        point = payload["points"][0]
        self.assertEqual(point["metricName"], "request_duration_ms")
        self.assertEqual(point["serviceName"], "checkout-api")
        self.assertEqual(point["value"], 142.8)
        self.assertNotIn("Z", point["capturedAt"])

    @patch("devpulse_client.urlopen", return_value=FakeResponse())
    def test_log_includes_service_environment_and_message(self, urlopen):
        status = self.client.ingest_log("error", "Provider timeout")

        self.assertEqual(status, 201)
        request = urlopen.call_args.args[0]
        self.assertEqual(request.full_url, "http://localhost:8080/api/projects/42/logs")
        self.assertEqual(request.get_header("X-api-key"), "dp_ing_test-secret")
        entry = json.loads(request.data)["entries"][0]
        self.assertEqual(entry["level"], "ERROR")
        self.assertEqual(entry["environment"], "staging")
        self.assertEqual(entry["message"], "Provider timeout")

    def test_requires_https_for_remote_servers(self):
        with self.assertRaisesRegex(ValueError, "Use HTTPS"):
            DevPulseClient("http://devpulse.company.test", 42, "key", "checkout-api")


if __name__ == "__main__":
    unittest.main()
