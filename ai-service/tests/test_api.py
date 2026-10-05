from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_and_ready_endpoints():
    assert client.get("/health").json()["status"] == "UP"
    assert client.get("/ready").json()["status"] == "READY"


def test_analyze_incidents_matches_java_contract():
    """The response keys here must match backend/.../IncidentAnalysisResponse.java
    exactly (classification, confidence, rootCause, recommendations, signals),
    since Spring's RestClient deserializes by field name."""
    response = client.post(
        "/api/analysis/incidents",
        json={
            "title": "Order API errors",
            "description": "Database connection timeout after latest deploy",
            "severity": "HIGH",
            "service": "order-api",
            "recent_logs": ["Connection to postgres timed out"],
        },
    )
    assert response.status_code == 200
    body = response.json()
    assert set(body.keys()) == {"classification", "confidence", "rootCause", "recommendations", "signals"}
    assert body["classification"] == "DATABASE"


def test_analyze_incidents_rejects_missing_title():
    response = client.post("/api/analysis/incidents", json={"description": "no title provided"})
    assert response.status_code == 422


def test_detect_anomalies_matches_expected_camel_case_contract():
    response = client.post(
        "/api/analysis/anomalies",
        json={"history": [10, 11, 9, 10, 12], "value": 80, "threshold": 3.0},
    )
    assert response.status_code == 200
    body = response.json()
    assert set(body.keys()) == {"isAnomaly", "zScore", "mean", "stdDev", "reason"}
    assert body["isAnomaly"] is True


def test_detect_anomalies_serializes_zero_variance_outlier_as_valid_json():
    response = client.post(
        "/api/analysis/anomalies",
        json={"history": [10, 10, 10], "value": 15},
    )
    assert response.status_code == 200
    assert response.json()["zScore"] is None
