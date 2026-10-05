-- V2__seed_default_roles.sql only seeded ADMIN and DEVELOPER; VIEWER (the third
-- RBAC role used throughout the app - see SecurityConfig / ProjectController) was
-- missing from the Flyway-managed schema. This completes the role set.
INSERT INTO roles (name) VALUES ('VIEWER') ON CONFLICT (name) DO NOTHING;
