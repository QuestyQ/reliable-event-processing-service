CREATE TABLE events (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    correlation_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uq_events_idempotency_key ON events (idempotency_key);
CREATE INDEX idx_events_status ON events (status);

CREATE TABLE dlq_events (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES events(id),
    reason TEXT NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
