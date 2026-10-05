-- Links an incident to a registered Service when one is known, while keeping
-- the free-text "service" column for incidents reported before the service
-- was formally registered (e.g. by an external monitoring agent).
ALTER TABLE incidents ADD COLUMN service_id BIGINT;
ALTER TABLE incidents ADD CONSTRAINT fk_incidents_service FOREIGN KEY (service_id) REFERENCES services(id);
