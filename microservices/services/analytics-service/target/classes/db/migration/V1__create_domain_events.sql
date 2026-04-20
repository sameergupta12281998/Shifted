CREATE TABLE domain_events (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type   VARCHAR(64)   NOT NULL,
    source_id    VARCHAR(128),
    amount       NUMERIC(14,2),
    vehicle_type VARCHAR(32),
    driver_id    VARCHAR(128),
    payload      TEXT,
    received_at  TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_domain_events_type       ON domain_events (event_type);
CREATE INDEX idx_domain_events_type_time  ON domain_events (event_type, received_at);
CREATE INDEX idx_domain_events_driver     ON domain_events (driver_id, received_at);
