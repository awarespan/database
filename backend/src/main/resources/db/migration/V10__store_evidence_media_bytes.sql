-- Store small annotated evidence images directly in PostgreSQL.
-- The 10 MB upload limit is enforced by EvidenceService.
ALTER TABLE evidence ADD COLUMN IF NOT EXISTS media_data BYTEA;
