from app.models.analysis import AnalysisRequest, AnalysisResponse


class IncidentAnalyzer:
    """Deterministic baseline analyzer; optional model enrichment can be added later."""

    _rules = (
        ("database", ("database", "postgres", "jdbc", "sql", "connection"), "DATABASE", "The incident signals a database connectivity or query problem.", ["Check database availability and connection pool saturation.", "Review recent schema or migration changes."]),
        ("memory", ("out of memory", "oom", "heap", "memory"), "RESOURCE", "The incident signals memory pressure in the affected service.", ["Inspect heap and container memory usage.", "Check for a recent increase in payload or traffic volume."]),
        ("latency", ("timeout", "latency", "slow", "response time", "5xx"), "PERFORMANCE", "The incident signals elevated latency or request failures.", ["Inspect p95/p99 latency and downstream dependencies.", "Compare the incident start time with the latest deployment."]),
    )

    def analyze(self, request: AnalysisRequest) -> AnalysisResponse:
        content = " ".join([request.title, request.description, *(request.recent_logs or [])]).lower()
        for _, keywords, classification, root_cause, recommendations in self._rules:
            signals = [keyword for keyword in keywords if keyword in content]
            if signals:
                return AnalysisResponse(classification=classification, confidence=min(0.95, 0.62 + len(signals) * 0.08), root_cause=root_cause, recommendations=recommendations, signals=signals)

        return AnalysisResponse(
            classification="GENERAL",
            confidence=0.45,
            root_cause="No deterministic signature matched the supplied incident context.",
            recommendations=["Collect recent logs and metrics around the incident window.", "Compare the affected service with its latest deployment."],
            signals=[],
        )
