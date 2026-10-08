from fastapi import APIRouter

from app.models.analysis import AnalysisRequest, AnalysisResponse
from app.services.incident_analyzer import IncidentAnalyzer

router = APIRouter(prefix="/api/analysis", tags=["analysis"])
analyzer = IncidentAnalyzer()


@router.post("/incidents", response_model=AnalysisResponse)
def analyze_incident(request: AnalysisRequest) -> AnalysisResponse:
    # Validation (required/blank/too long fields) is handled by FastAPI and returns a 422.
    return analyzer.analyze(request)
