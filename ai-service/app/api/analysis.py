from fastapi import APIRouter, HTTPException

from app.models.analysis import AnalysisRequest, AnalysisResponse
from app.services.incident_analyzer import IncidentAnalyzer

router = APIRouter(prefix="/api/analysis", tags=["analysis"])
analyzer = IncidentAnalyzer()


@router.post("/incidents", response_model=AnalysisResponse)
def analyze_incident(payload: dict) -> AnalysisResponse:
    title = payload.get("title")
    description = payload.get("description")
    if not isinstance(title, str) or not title.strip() or not isinstance(description, str) or not description.strip():
        raise HTTPException(status_code=422, detail="title and description are required")

    request = AnalysisRequest(
        title=title,
        description=description,
        severity=payload.get("severity") or "MEDIUM",
        service=payload.get("service"),
        recent_logs=payload.get("recent_logs") or payload.get("recentLogs") or [],
    )
    return analyzer.analyze(request)
