from fastapi import FastAPI
from app.api.analysis import router as analysis_router
from app.api.anomaly import router as anomaly_router

app = FastAPI(title="DevPulse AI Service", version="0.1.0")
app.include_router(analysis_router)
app.include_router(anomaly_router)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP", "service": "ai-service"}


@app.get("/ready")
def ready() -> dict[str, str]:
    return {"status": "READY", "service": "ai-service"}
