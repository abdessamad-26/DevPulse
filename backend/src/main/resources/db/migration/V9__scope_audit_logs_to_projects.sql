ALTER TABLE audit_logs
    ADD COLUMN project_id BIGINT REFERENCES projects(id) ON DELETE SET NULL;

CREATE INDEX idx_audit_logs_project_created_at
    ON audit_logs(project_id, created_at DESC, id DESC);

CREATE INDEX idx_audit_logs_created_at
    ON audit_logs(created_at DESC, id DESC);
