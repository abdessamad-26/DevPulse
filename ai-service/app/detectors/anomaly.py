"""Simple statistical anomaly detector (z-score against historical values).

This is the "stats" layer referenced in docs/architecture.md's AI design
(rules -> stats -> optional LLM): a lightweight, dependency-free complement
to the keyword-based IncidentAnalyzer, useful for flagging a metric value
that deviates sharply from its recent history without needing a trained model.
"""
import math
import statistics


def z_score_anomaly(history: list[float], value: float, threshold: float = 3.0) -> dict:
    """Returns whether `value` is an outlier relative to `history`.

    Uses population standard deviation (pstdev) since `history` is treated as
    the complete recent window being evaluated, not a sample of a larger population.
    """
    if len(history) < 2:
        return {
            "is_anomaly": False,
            "z_score": 0.0,
            "mean": value,
            "std_dev": 0.0,
            "reason": "Not enough history to evaluate (need at least 2 points).",
        }

    mean = statistics.mean(history)
    std_dev = statistics.pstdev(history)

    if std_dev == 0:
        is_anomaly = value != mean
        z = None if is_anomaly else 0.0
        reason = (
            "History has zero variance; any different value is flagged with an unbounded z-score."
            if is_anomaly
            else "Value matches constant history."
        )
        return {"is_anomaly": is_anomaly, "z_score": z, "mean": mean, "std_dev": std_dev, "reason": reason}

    z = (value - mean) / std_dev
    is_anomaly = abs(z) > threshold
    if not math.isfinite(z):
        return {
            "is_anomaly": is_anomaly,
            "z_score": None,
            "mean": mean,
            "std_dev": std_dev,
            "reason": "The calculated z-score is unbounded and cannot be represented as a finite JSON number.",
        }
    reason = f"|z-score|={abs(z):.2f} {'exceeds' if is_anomaly else 'is within'} threshold {threshold}."
    return {"is_anomaly": is_anomaly, "z_score": z, "mean": mean, "std_dev": std_dev, "reason": reason}
