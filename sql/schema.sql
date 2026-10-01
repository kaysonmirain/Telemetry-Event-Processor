CREATE TABLE IF NOT EXISTS telemetry_event (
    event_key TEXT PRIMARY KEY,
    time_seconds DOUBLE PRECISION NOT NULL CHECK (time_seconds >= 0),
    agent_id INTEGER NOT NULL,
    from_node INTEGER NOT NULL,
    to_node INTEGER NOT NULL,
    progress DOUBLE PRECISION NOT NULL CHECK (progress BETWEEN 0 AND 1),
    completed BOOLEAN NOT NULL,
    ingested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS telemetry_event_agent_time_idx
    ON telemetry_event (agent_id, time_seconds);

CREATE INDEX IF NOT EXISTS telemetry_event_destination_idx
    ON telemetry_event (to_node, time_seconds);
