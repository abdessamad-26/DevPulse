from pydantic import BaseModel, ConfigDict, Field


class AnomalyRequest(BaseModel):
    history: list[float] = Field(default_factory=list)
    value: float
    threshold: float = Field(default=3.0, gt=0)


class AnomalyResponse(BaseModel):
    model_config = ConfigDict(serialize_by_alias=True)

    is_anomaly: bool = Field(serialization_alias="isAnomaly")
    z_score: float = Field(serialization_alias="zScore")
    mean: float
    std_dev: float = Field(serialization_alias="stdDev")
    reason: str
