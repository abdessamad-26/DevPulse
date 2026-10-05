from fastapi import APIRouter

from app.detectors.anomaly import z_score_anomaly
from app.models.anomaly import AnomalyRequest, AnomalyResponse

router = APIRouter(prefix="/api/analysis", tags=["analysis"])


@router.post("/anomalies", response_model=AnomalyResponse)
def detect_anomaly(request: AnomalyRequest) -> AnomalyResponse:
    result = z_score_anomaly(request.history, request.value, request.threshold)
    return AnomalyResponse(**result)
