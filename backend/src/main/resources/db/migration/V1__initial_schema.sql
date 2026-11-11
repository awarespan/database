-- V1: Initial schema for SIP Backend
-- Neon PostgreSQL

CREATE TABLE users (
    id TEXT PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'OPERATOR',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_role_check CHECK (role IN ('ADMIN', 'OPERATOR'))
);

CREATE TABLE nodes (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'UNKNOWN',
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    last_heartbeat TIMESTAMPTZ,
    battery_level INTEGER,
    firmware_version TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT nodes_status_check CHECK (status IN ('ONLINE', 'DEGRADED', 'OFFLINE', 'UNKNOWN'))
);

CREATE TABLE incidents (
    id TEXT PRIMARY KEY,
    state TEXT NOT NULL DEFAULT 'DETECTED',
    threat_type TEXT NOT NULL,
    threat_severity TEXT NOT NULL,
    threat_description TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    location_readable TEXT,
    location_accuracy DOUBLE PRECISION,
    node_id TEXT NOT NULL REFERENCES nodes(id),
    autopilot_handled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT incidents_state_check CHECK (state IN ('DETECTED','PENDING_VERIFICATION','VERIFIED','REJECTED','ESCALATED','RESOLVED')),
    CONSTRAINT incidents_threat_type_check CHECK (threat_type IN ('WEAPON','SUSPICIOUS_BEHAVIOR','AUDIO_ANOMALY','FIRE','UNKNOWN')),
    CONSTRAINT incidents_threat_severity_check CHECK (threat_severity IN ('LOW','MEDIUM','HIGH','NON_DETERRABLE'))
);

CREATE TABLE detection_results (
    id TEXT PRIMARY KEY,
    incident_id TEXT NOT NULL UNIQUE REFERENCES incidents(id) ON DELETE CASCADE,
    predicted_class TEXT NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    model_version TEXT NOT NULL,
    detection_timestamp TIMESTAMPTZ NOT NULL,
    sensor_modalities JSONB,
    raw_scores JSONB
);

CREATE TABLE human_annotations (
    id TEXT PRIMARY KEY,
    incident_id TEXT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    annotator_id TEXT NOT NULL REFERENCES users(id),
    label TEXT NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    notes TEXT,
    confidence DOUBLE PRECISION,
    version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT annotations_label_check CHECK (label IN ('FALSE_POSITIVE','TRUE_POSITIVE','UNCERTAIN'))
);

CREATE TABLE evidence (
    id TEXT PRIMARY KEY,
    incident_id TEXT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    type TEXT NOT NULL,
    storage_key TEXT,
    mime_type TEXT,
    size_bytes BIGINT,
    sha256 TEXT,
    capture_timestamp TIMESTAMPTZ,
    retention_expiry TIMESTAMPTZ,
    upload_status TEXT NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT evidence_type_check CHECK (type IN ('IMAGE','VIDEO','AUDIO','THUMBNAIL')),
    CONSTRAINT evidence_upload_status_check CHECK (upload_status IN ('PENDING','UPLOADED','FAILED'))
);

CREATE TABLE response_events (
    id TEXT PRIMARY KEY,
    incident_id TEXT NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    action_type TEXT NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    result TEXT,
    autonomous BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT response_action_type_check CHECK (action_type IN ('ALERT','DETERRENCE','ESCALATION','VERIFICATION','REJECTION','RESOLUTION'))
);

CREATE TABLE audit_events (
    id TEXT PRIMARY KEY,
    incident_id TEXT REFERENCES incidents(id) ON DELETE SET NULL,
    event_type TEXT NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    description TEXT,
    actor TEXT
);

CREATE TABLE autopilot_policies (
    node_id TEXT PRIMARY KEY REFERENCES nodes(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    allowed_threat_types JSONB NOT NULL DEFAULT '[]'::jsonb,
    confidence_threshold DOUBLE PRECISION NOT NULL DEFAULT 0.85,
    deterrence_timeout_seconds INTEGER NOT NULL DEFAULT 5,
    auto_escalate_on_deterrence_failure BOOLEAN NOT NULL DEFAULT TRUE,
    max_deterrence_attempts INTEGER NOT NULL DEFAULT 1,
    always_escalate_types JSONB NOT NULL DEFAULT '[]'::jsonb,
    require_minimum_confidence BOOLEAN NOT NULL DEFAULT TRUE,
    require_multi_modal_confirmation BOOLEAN NOT NULL DEFAULT FALSE,
    never_autonomous_types JSONB NOT NULL DEFAULT '[]'::jsonb,
    sync_state TEXT NOT NULL DEFAULT 'DISABLED',
    last_sync TIMESTAMPTZ
);

CREATE TABLE model_versions (
    version TEXT PRIMARY KEY,
    model_name TEXT NOT NULL,
    trained_at TIMESTAMPTZ,
    dataset_hash TEXT,
    validation_map50 DOUBLE PRECISION
);

CREATE TABLE refresh_tokens (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash TEXT NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);