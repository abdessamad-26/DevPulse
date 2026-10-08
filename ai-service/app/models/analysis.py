from typing import Annotated

from pydantic import AliasChoices, BaseModel, ConfigDict, Field, StringConstraints, field_validator

MAX_RECENT_LOGS = 200
MAX_LOG_LINE_LENGTH = 2000


class AnalysisRequest(BaseModel):
    title: str = Field(min_length=1, max_length=255)
    description: str = Field(min_length=1, max_length=10000)
    severity: str = Field(default="MEDIUM", max_length=30)
    service: str | None = Field(default=None, max_length=150)
    # The Java backend sends `recent_logs`; `recentLogs` is accepted as well for camelCase clients.
    recent_logs: list[Annotated[str, StringConstraints(max_length=MAX_LOG_LINE_LENGTH)]] | None = Field(
        default=None,
        max_length=MAX_RECENT_LOGS,
        validation_alias=AliasChoices("recent_logs", "recentLogs"),
    )

    @field_validator("title", "description")
    @classmethod
    def must_not_be_blank(cls, value: str) -> str:
        if not value.strip():
            raise ValueError("must not be blank")
        return value

    @field_validator("severity", mode="before")
    @classmethod
    def default_severity(cls, value: object) -> object:
        # An explicit null or empty severity falls back to MEDIUM (same contract as before).
        return value or "MEDIUM"


class AnalysisResponse(BaseModel):
    model_config = ConfigDict(serialize_by_alias=True)

    classification: str
    confidence: float
    root_cause: str = Field(serialization_alias="rootCause")
    recommendations: list[str]
    signals: list[str]
