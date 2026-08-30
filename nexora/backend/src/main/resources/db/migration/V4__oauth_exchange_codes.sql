-- One-time OAuth exchange codes (JWT handoff after Google callback).
CREATE TABLE IF NOT EXISTS oauth_exchange_codes (
    code VARCHAR(64) PRIMARY KEY,
    payload TEXT NOT NULL,
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_oauth_exchange_expires ON oauth_exchange_codes (expires_at);
