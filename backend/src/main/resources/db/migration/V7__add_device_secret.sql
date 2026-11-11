-- V7: Add device_secret column to nodes table
-- Required by Node.entity deviceSecret field used in PiAuthenticationService

ALTER TABLE nodes ADD COLUMN IF NOT EXISTS device_secret TEXT;