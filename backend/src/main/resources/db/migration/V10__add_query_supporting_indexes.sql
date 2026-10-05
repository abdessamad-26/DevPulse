CREATE INDEX idx_projects_owner_created
    ON projects(owner_id, created_at DESC, id DESC);

CREATE INDEX idx_services_project_created
    ON services(project_id, created_at DESC, id DESC);

CREATE INDEX idx_incidents_project_created
    ON incidents(project_id, created_at DESC, id DESC);

CREATE INDEX idx_deployments_project_created
    ON deployments(project_id, created_at DESC, id DESC);

CREATE INDEX idx_alert_rules_project_created
    ON alert_rules(project_id, created_at DESC, id DESC);

CREATE INDEX idx_alerts_rule_created
    ON alerts(alert_rule_id, created_at DESC, id DESC);

CREATE INDEX idx_metrics_project_captured
    ON metrics(project_id, captured_at DESC, id DESC);

CREATE INDEX idx_metrics_project_name_captured
    ON metrics(project_id, metric_name, captured_at ASC, id ASC);

CREATE INDEX idx_logs_project_timestamp
    ON logs(project_id, timestamp DESC, id DESC);

CREATE INDEX idx_chaos_project_created
    ON chaos_simulations(project_id, created_at DESC, id DESC);
