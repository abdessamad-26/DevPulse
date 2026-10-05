from app.detectors.anomaly import z_score_anomaly


def test_flags_clear_outlier():
    history = [10, 11, 9, 10, 12, 10, 11]
    result = z_score_anomaly(history, 50)
    assert result["is_anomaly"] is True
    assert result["z_score"] > 3.0


def test_does_not_flag_normal_value():
    history = [10, 11, 9, 10, 12, 10, 11]
    result = z_score_anomaly(history, 10.5)
    assert result["is_anomaly"] is False


def test_handles_insufficient_history():
    result = z_score_anomaly([5.0], 100.0)
    assert result["is_anomaly"] is False
    assert "Not enough history" in result["reason"]


def test_handles_zero_variance_history_with_matching_value():
    result = z_score_anomaly([10.0, 10.0, 10.0], 10.0)
    assert result["is_anomaly"] is False


def test_handles_zero_variance_history_with_different_value():
    result = z_score_anomaly([10.0, 10.0, 10.0], 15.0)
    assert result["is_anomaly"] is True
    assert result["z_score"] == float("inf")


def test_respects_custom_threshold():
    history = [10, 11, 9, 10, 12, 10, 11]
    result_strict = z_score_anomaly(history, 15, threshold=1.0)
    assert result_strict["is_anomaly"] is True

    result_loose = z_score_anomaly(history, 15, threshold=10.0)
    assert result_loose["is_anomaly"] is False
