-- Background jobs, sync locks, Gmail watch, user prefs, calendar links, action snooze.
-- Postgres IF NOT EXISTS (also accepted by H2 in MySQL mode used locally).

CREATE TABLE IF NOT EXISTS background_jobs (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT,
    job_type        VARCHAR(64) NOT NULL,
    payload         TEXT,
    status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    attempts        INT NOT NULL DEFAULT 0,
    run_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_at       TIMESTAMPTZ,
    last_error      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_background_jobs_due
    ON background_jobs (status, run_at);

CREATE TABLE IF NOT EXISTS user_sync_locks (
    user_id         BIGINT PRIMARY KEY,
    locked_until    TIMESTAMPTZ,
    lock_owner      VARCHAR(128)
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS watch_expiration TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN IF NOT EXISTS watch_resource_id VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS quiet_hours_start INT NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS quiet_hours_end INT NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS muted_categories TEXT NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS digest_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS digest_hour INT DEFAULT 8;

ALTER TABLE emails ADD COLUMN IF NOT EXISTS calendar_html_link VARCHAR(1024) NULL;
ALTER TABLE emails ADD COLUMN IF NOT EXISTS google_event_id VARCHAR(255) NULL;

ALTER TABLE email_actions ADD COLUMN IF NOT EXISTS snoozed_until TIMESTAMPTZ NULL;
