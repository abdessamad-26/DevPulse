from pydantic import BaseModel, ConfigDict, Field


class AnalysisRequest(BaseModel):
    title: str = Field(min_length=1, max_length=255)
    description: str = Field(min_length=1, max_length=10000)
    severity: str = Field(default="MEDIUM", max_length=30)
    service: str | None = Field(default=None, max_length=150)
    recent_logs: list[str] | None = None


class AnalysisResponse(BaseModel):
    model_config = ConfigDict(serialize_by_alias=True)

    classification: str
    confidence: float
    root_cause: str = Field(serialization_alias="rootCause")
    recommendations: list[str]
    signals: list[str]
