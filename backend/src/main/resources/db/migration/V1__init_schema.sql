CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    repository VARCHAR(500),
    environment VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_projects_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE TABLE services (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    type VARCHAR(100),
    health_status VARCHAR(50) NOT NULL DEFAULT 'HEALTHY',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_services_project FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE TABLE incidents (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    severity VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    service VARCHAR(150),
    project_id BIGINT,
    started_at TIMESTAMP,
    resolved_at TIMESTAMP,
    detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    root_cause TEXT,
    recommendations TEXT,
    confidence_score DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_incidents_project FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE TABLE deployments (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    version VARCHAR(100) NOT NULL,
    commit VARCHAR(255),
    branch VARCHAR(100),
    environment VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    author VARCHAR(150),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_deployments_project FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE TABLE alert_rules (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    metric VARCHAR(100) NOT NULL,
    operator VARCHAR(20) NOT NULL,
    threshold DOUBLE PRECISION NOT NULL,
    duration VARCHAR(50),
    severity VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    project_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_alert_rules_project FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE TABLE alerts (
    id BIGSERIAL PRIMARY KEY,
    alert_rule_id BIGINT NOT NULL,
    message TEXT NOT NULL,
    severity VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_alerts_rule FOREIGN KEY (alert_rule_id) REFERENCES alert_rules(id)
);

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(150) NOT NULL,
    entity_type VARCHAR(150),
    entity_id BIGINT,
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE metrics (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    service_name VARCHAR(150),
    metric_name VARCHAR(150) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    unit VARCHAR(50),
    captured_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_metrics_project FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE TABLE logs (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    service_name VARCHAR(150),
    environment VARCHAR(50),
    level VARCHAR(50),
    message TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_logs_project FOREIGN KEY (project_id) REFERENCES projects(id)
);
