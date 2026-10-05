CREATE TABLE chaos_simulations (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id),
    action VARCHAR(50) NOT NULL,
    target_service VARCHAR(150),
    triggered_by VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'TRIGGERED',
    resulting_incident_id BIGINT REFERENCES incidents(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
