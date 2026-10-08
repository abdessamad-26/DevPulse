from pydantic import BaseModel, ConfigDict, Field, FiniteFloat

MAX_HISTORY_POINTS = 10_000


class AnomalyRequest(BaseModel):
    history: list[FiniteFloat] = Field(default_factory=list, max_length=MAX_HISTORY_POINTS)
    value: FiniteFloat
    threshold: FiniteFloat = Field(default=3.0, gt=0)


class AnomalyResponse(BaseModel):
    model_config = ConfigDict(serialize_by_alias=True)

    is_anomaly: bool = Field(serialization_alias="isAnomaly")
    z_score: float | None = Field(serialization_alias="zScore")
    mean: float
    std_dev: float = Field(serialization_alias="stdDev")
    reason: str
