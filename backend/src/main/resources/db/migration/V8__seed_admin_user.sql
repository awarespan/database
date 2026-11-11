-- V8: Seed admin user directly via Flyway (fallback if BootstrapAdminRunner fails)
-- This migration runs AFTER all schema migrations and ensures at least one admin exists.
-- Password: S1pGuardian2026!
-- BCrypt cost: 12 (matches SecurityConfig.passwordEncoder)
--
-- Generated with: bcrypt.hashSync('S1pGuardian2026!', 12)

INSERT INTO users (id, username, password_hash, role, enabled, created_at, updated_at)
VALUES (
    'admin-bootstrap-001',
    'bchbenjamin',
    '$2b$12$E3IyCWXRnIn7uki.g4xUVOkSWfhLthhD37kBtDrJqiln7zRUVJ8YC',
    'ADMIN',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (username) DO NOTHING;