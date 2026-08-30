-- Session revoke: bump token_version to invalidate outstanding JWTs.
ALTER TABLE users ADD COLUMN IF NOT EXISTS token_version INTEGER NOT NULL DEFAULT 0;
