-- Backend-owned node identity and idempotent installation enrollment.
ALTER TABLE nodes ADD COLUMN IF NOT EXISTS installation_id TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS ux_nodes_installation_id
    ON nodes (installation_id)
    WHERE installation_id IS NOT NULL;
