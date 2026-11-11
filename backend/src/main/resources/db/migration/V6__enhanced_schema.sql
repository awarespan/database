-- V6: Enhanced schema - Pi integration, command queue, heartbeats, datasets, training, notifications
-- Required for MQTT-based Pi communication, offline sync, and model training pipeline

-- Node heartbeat tracking for liveness detection
CREATE TABLE node_heartbeats (
    id TEXT PRIMARY KEY,
    node_id TEXT NOT NULL REFERENCES nodes(id) ON DELETE CASCADE,
    timestamp TIMESTAMPTZ NOT NULL,
    battery_level INTEGER,
    temperature DOUBLE PRECISION,
    storage_available_mb BIGINT,
    model_version TEXT,
    queue_depth INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT node_heartbeats_unique UNIQUE (node_id, timestamp)
);
CREATE INDEX idx_heartbeats_node_time ON node_heartbeats(node_id, timestamp DESC);
CREATE INDEX idx_heartbeats_timestamp ON node_heartbeats(timestamp);

-- Per-node MQTT credentials (derived from node_id + salt, NOT stored secrets)
CREATE TABLE mqtt_node_credentials (
    node_id TEXT PRIMARY KEY REFERENCES nodes(id) ON DELETE CASCADE,
    credential_hash TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    rotated_at TIMESTAMPTZ,
    active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_mqtt_creds_hash ON mqtt_node_credentials(credential_hash);

-- Commands dispatched from backend to Pi (siren, model update, config sync, etc.)
CREATE TABLE command_queue (
    id TEXT PRIMARY KEY,
    node_id TEXT NOT NULL REFERENCES nodes(id) ON DELETE CASCADE,
    command_type TEXT NOT NULL,
    payload JSONB,
    priority INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dispatched_at TIMESTAMPTZ,
    acknowledged_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    failure_reason TEXT,
    CONSTRAINT cmd_queue_status_check CHECK (status IN ('PENDING','DISPATCHED','ACKNOWLEDGED','EXPIRED','FAILED'))
);
CREATE INDEX idx_command_queue_node_status ON command_queue(node_id, status);
CREATE INDEX idx_command_queue_created ON command_queue(created_at DESC);
CREATE INDEX idx_command_queue_expires ON command_queue(expires_at) WHERE expires_at IS NOT NULL;

-- Dataset version registry for training provenance tracking
CREATE TABLE dataset_versions (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    version TEXT NOT NULL,
    description TEXT,
    source_url TEXT,
    hash_sha256 TEXT,
    size_bytes BIGINT,
    num_images INTEGER,
    num_annotations INTEGER,
    tags JSONB DEFAULT '[]'::jsonb,
    metadata JSONB DEFAULT '{}'::jsonb,
    status TEXT NOT NULL DEFAULT 'REGISTERED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    validated_at TIMESTAMPTZ,
    CONSTRAINT dataset_versions_status_check CHECK (status IN ('REGISTERED','VALIDATING','VALID','INVALID','DEPRECATED'))
);
CREATE INDEX idx_dataset_versions_name_version ON dataset_versions(name, version);
CREATE INDEX idx_dataset_versions_status ON dataset_versions(status);

-- Training run history for model version lineage
CREATE TABLE training_runs (
    id TEXT PRIMARY KEY,
    model_name TEXT NOT NULL,
    base_model_version TEXT,
    dataset_version_id TEXT REFERENCES dataset_versions(id),
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    status TEXT NOT NULL DEFAULT 'RUNNING',
    epochs_requested INTEGER NOT NULL,
    epochs_completed INTEGER,
    batch_size INTEGER,
    learning_rate DOUBLE PRECISION,
    final_train_loss DOUBLE PRECISION,
    final_val_loss DOUBLE PRECISION,
    final_map50 DOUBLE PRECISION,
    final_map50_95 DOUBLE PRECISION,
    checkpoint_path TEXT,
    output_model_version TEXT,
    gpu_info JSONB,
    logs_s3_key TEXT,
    error_message TEXT,
    CONSTRAINT training_runs_status_check CHECK (status IN ('RUNNING','COMPLETED','FAILED','CANCELLED'))
);
CREATE INDEX idx_training_runs_model_name ON training_runs(model_name, started_at DESC);
CREATE INDEX idx_training_runs_status ON training_runs(status);

-- Notification delivery tracking (push, SMS, email)
CREATE TABLE notification_deliveries (
    id TEXT PRIMARY KEY,
    incident_id TEXT REFERENCES incidents(id) ON DELETE SET NULL,
    channel TEXT NOT NULL,
    recipient TEXT NOT NULL,
    payload JSONB DEFAULT '{}'::jsonb,
    status TEXT NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    failure_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT notification_deliveries_status_check CHECK (status IN ('PENDING','SENT','DELIVERED','FAILED','SUPPRESSED'))
);
CREATE INDEX idx_notification_deliveries_incident ON notification_deliveries(incident_id);
CREATE INDEX idx_notification_deliveries_status ON notification_deliveries(status, created_at DESC);

-- Add missing columns to incidents table for Pi sync and tier support
ALTER TABLE incidents ADD COLUMN IF NOT EXISTS sync_status TEXT NOT NULL DEFAULT 'SYNCED';
ALTER TABLE incidents ADD COLUMN IF NOT EXISTS idempotency_key TEXT UNIQUE;
ALTER TABLE incidents ADD COLUMN IF NOT EXISTS tier INTEGER NOT NULL DEFAULT 0;
ALTER TABLE incidents ADD COLUMN IF NOT EXISTS detection_domain TEXT;

ALTER TABLE incidents DROP CONSTRAINT IF EXISTS incidents_threat_type_check;
ALTER TABLE incidents ADD CONSTRAINT incidents_threat_type_check CHECK (
    threat_type IN ('WEAPON','SUSPICIOUS_BEHAVIOR','AUDIO_ANOMALY','FIRE','ANIMAL','VIOLENCE','VEHICLE','UNKNOWN')
);
ALTER TABLE incidents DROP CONSTRAINT IF EXISTS incidents_threat_severity_check;
ALTER TABLE incidents ADD CONSTRAINT incidents_threat_severity_check CHECK (
    threat_severity IN ('LOW','MEDIUM','HIGH','NON_DETERRABLE')
);

ALTER TABLE incidents ADD CONSTRAINT incidents_sync_status_check CHECK (
    sync_status IN ('SYNCED','PENDING','RETRY','FAILED')
);
ALTER TABLE incidents ADD CONSTRAINT incidents_tier_check CHECK (tier >= 0 AND tier <= 3);
ALTER TABLE incidents ADD CONSTRAINT incidents_detection_domain_check CHECK (
    detection_domain IN ('ANIMAL','VIOLENCE','WEAPON','UNKNOWN') OR detection_domain IS NULL
);

CREATE INDEX IF NOT EXISTS idx_incidents_sync_status ON incidents(sync_status);
CREATE INDEX IF NOT EXISTS idx_incidents_idempotency ON incidents(idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_incidents_tier ON incidents(tier);
CREATE INDEX IF NOT EXISTS idx_incidents_node_created ON incidents(node_id, created_at DESC);

-- Add indexes to existing tables that are likely missing
CREATE INDEX IF NOT EXISTS idx_evidence_incident ON evidence(incident_id);
CREATE INDEX IF NOT EXISTS idx_response_incident ON response_events(incident_id);
CREATE INDEX IF NOT EXISTS idx_response_timestamp ON response_events(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_incident ON audit_events(incident_id);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_events(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_nodes_status ON nodes(status);
CREATE INDEX IF NOT EXISTS idx_nodes_last_heartbeat ON nodes(last_heartbeat);