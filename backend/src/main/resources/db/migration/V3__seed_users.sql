-- V3: Seed development/test users
-- Password for all: ChangeMe123! (BCrypt hash)
-- Generated with: htpasswd -nbBC 12 "" ChangeMe123! | tr -d ':\n' | cut -c2-
-- The BCrypt hash below is: /X4.NUxV8OOO1gLfZW

INSERT INTO users (id, username, password_hash, role, enabled, created_at, updated_at)
VALUES
    ('user-admin-001', 'admin', '/X4.NUxV8OOO1gLfZW', 'ADMIN', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('user-operator-001', 'operator', '/X4.NUxV8OOO1gLfZW', 'OPERATOR', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Seed a sample node for development
INSERT INTO nodes (id, name, status, latitude, longitude, last_heartbeat, created_at, updated_at)
VALUES ('node-01', 'Demo Edge Node 1', 'OFFLINE', 12.9716, 77.5946, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;