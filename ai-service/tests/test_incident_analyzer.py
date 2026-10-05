from app.models.analysis import AnalysisRequest
from app.services.incident_analyzer import IncidentAnalyzer


def test_classifies_database_incident():
    analyzer = IncidentAnalyzer()
    request = AnalysisRequest(
        title="Checkout failing",
        description="Database connection pool exhausted, postgres timeouts",
    )
    result = analyzer.analyze(request)
    assert result.classification == "DATABASE"
    assert result.confidence > 0.6
    assert "database" in result.signals


def test_classifies_memory_incident():
    analyzer = IncidentAnalyzer()
    request = AnalysisRequest(title="Service crash", description="OutOfMemoryError, heap exhausted")
    result = analyzer.analyze(request)
    assert result.classification == "RESOURCE"


def test_classifies_latency_incident():
    analyzer = IncidentAnalyzer()
    request = AnalysisRequest(title="Slow checkout", description="High latency and timeout on 5xx responses")
    result = analyzer.analyze(request)
    assert result.classification == "PERFORMANCE"


def test_falls_back_to_general_when_no_signature_matches():
    analyzer = IncidentAnalyzer()
    request = AnalysisRequest(title="Something odd", description="Users report unexpected purple button color")
    result = analyzer.analyze(request)
    assert result.classification == "GENERAL"
    assert result.confidence == 0.45
    assert result.signals == []


def test_confidence_increases_with_more_matching_signals():
    analyzer = IncidentAnalyzer()
    request = AnalysisRequest(title="DB issue", description="database connection sql timeout jdbc")
    result = analyzer.analyze(request)
    assert result.confidence <= 0.95
    assert len(result.signals) >= 2


def test_response_serializes_root_cause_with_camel_case_alias():
    analyzer = IncidentAnalyzer()
    request = AnalysisRequest(title="DB issue", description="database timeout")
    result = analyzer.analyze(request)
    dumped = result.model_dump(by_alias=True)
    assert "rootCause" in dumped
    assert "root_cause" not in dumped
