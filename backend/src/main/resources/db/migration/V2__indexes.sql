-- V2: Indexes for query performance

CREATE INDEX idx_incidents_created_at ON incidents (created_at DESC);
CREATE INDEX idx_incidents_state ON incidents (state);
CREATE INDEX idx_incidents_node_id ON incidents (node_id);
CREATE INDEX idx_incidents_threat_type ON incidents (threat_type);
CREATE INDEX idx_incidents_updated_at ON incidents (updated_at DESC);
CREATE INDEX idx_annotations_incident_id ON human_annotations (incident_id);
CREATE INDEX idx_response_events_incident_id ON response_events (incident_id);
CREATE INDEX idx_audit_events_incident_id ON audit_events (incident_id);
CREATE INDEX idx_audit_events_timestamp ON audit_events (timestamp DESC);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens (expires_at);