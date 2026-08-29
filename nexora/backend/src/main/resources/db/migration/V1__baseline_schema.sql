-- Cortex Mail baseline schema (PostgreSQL / Supabase)
-- Gmail-native identity is keyed by (user_id, gmail_message_id).

CREATE TABLE IF NOT EXISTS users (
    id                      BIGSERIAL PRIMARY KEY,
    google_id               VARCHAR(255) NOT NULL UNIQUE,
    email                   VARCHAR(255) NOT NULL UNIQUE,
    name                    VARCHAR(255) NOT NULL,
    profile_picture_url     TEXT,
    user_role               VARCHAR(64),
    gmail_access_token      TEXT,
    gmail_refresh_token     TEXT,
    token_expiry            TIMESTAMP,
    created_at              TIMESTAMP,
    last_synced_at          TIMESTAMP,
    gmail_label_counts      TEXT,
    gmail_history_id        VARCHAR(255),
    calendar_sync_enabled   BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS emails (
    id                              BIGSERIAL PRIMARY KEY,
    user_id                         BIGINT NOT NULL REFERENCES users(id),
    gmail_message_id                VARCHAR(255) NOT NULL,
    gmail_thread_id                 VARCHAR(255),
    sender_name                     VARCHAR(512),
    sender_email                    VARCHAR(512) NOT NULL,
    subject                         TEXT,
    body_snippet                    TEXT,
    body_full                       TEXT,
    body_html                       TEXT,
    received_at                     TIMESTAMP,
    is_read                         BOOLEAN DEFAULT FALSE,
    has_attachments                 BOOLEAN DEFAULT FALSE,
    gmail_label_ids                 TEXT,
    recipient_to                    TEXT,
    recipient_cc                    TEXT,
    recipient_bcc                   TEXT,
    reply_to                        VARCHAR(512),
    size_estimate                   BIGINT,
    is_starred                      BOOLEAN DEFAULT FALSE,
    is_important                    BOOLEAN DEFAULT FALSE,
    in_inbox                        BOOLEAN DEFAULT TRUE,
    is_draft                        BOOLEAN DEFAULT FALSE,
    is_archived                     BOOLEAN DEFAULT FALSE,
    is_trash                        BOOLEAN DEFAULT FALSE,
    is_spam                         BOOLEAN DEFAULT FALSE,
    category                        VARCHAR(64),
    priority                        VARCHAR(32),
    reaction                        VARCHAR(32),
    ai_summary                      TEXT,
    ai_action_items                 TEXT,
    deadline_detected               TIMESTAMP,
    is_deadline_added_to_calendar   BOOLEAN DEFAULT FALSE,
    created_at                      TIMESTAMP,
    CONSTRAINT uk_emails_user_gmail_message UNIQUE (user_id, gmail_message_id)
);

CREATE INDEX IF NOT EXISTS idx_emails_user_category ON emails(user_id, category);
CREATE INDEX IF NOT EXISTS idx_emails_user_priority ON emails(user_id, priority);
CREATE INDEX IF NOT EXISTS idx_emails_user_received ON emails(user_id, received_at DESC);
CREATE INDEX IF NOT EXISTS idx_emails_user_thread ON emails(user_id, gmail_thread_id);
CREATE INDEX IF NOT EXISTS idx_emails_user_inbox_received ON emails(user_id, in_inbox, received_at DESC);
CREATE INDEX IF NOT EXISTS idx_emails_user_unread ON emails(user_id, is_read, in_inbox);
CREATE INDEX IF NOT EXISTS idx_emails_user_deadline ON emails(user_id, deadline_detected);
CREATE INDEX IF NOT EXISTS idx_emails_user_archived ON emails(user_id, is_archived, received_at DESC);

CREATE TABLE IF NOT EXISTS email_attachments (
    id                      BIGSERIAL PRIMARY KEY,
    email_id                BIGINT NOT NULL REFERENCES emails(id) ON DELETE CASCADE,
    gmail_attachment_id     VARCHAR(512),
    filename                VARCHAR(1024),
    mime_type               VARCHAR(255),
    size_bytes              BIGINT,
    content_id              VARCHAR(512),
    is_inline               BOOLEAN DEFAULT FALSE,
    created_at              TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_email_attachments_email ON email_attachments(email_id);

CREATE TABLE IF NOT EXISTS email_actions (
    id                      BIGSERIAL PRIMARY KEY,
    email_id                BIGINT NOT NULL REFERENCES emails(id),
    user_id                 BIGINT NOT NULL,
    action_type             VARCHAR(64) NOT NULL,
    action_description      TEXT NOT NULL,
    deadline                TIMESTAMP,
    is_completed            BOOLEAN DEFAULT FALSE,
    created_at              TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_email_actions_user ON email_actions(user_id, is_completed);

CREATE TABLE IF NOT EXISTS email_drafts (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL REFERENCES users(id),
    to_address              TEXT,
    cc                      TEXT,
    bcc                     TEXT,
    subject                 TEXT,
    body                    TEXT,
    html_body               TEXT,
    scheduled_send_time     BIGINT,
    draft_status            VARCHAR(64),
    created_at              TIMESTAMP,
    updated_at              TIMESTAMP
);

CREATE TABLE IF NOT EXISTS email_templates (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL REFERENCES users(id),
    name                    VARCHAR(255) NOT NULL,
    subject                 TEXT,
    body                    TEXT,
    html_body               TEXT,
    category                TEXT,
    usage_count             INTEGER,
    created_at              TIMESTAMP,
    updated_at              TIMESTAMP
);

CREATE TABLE IF NOT EXISTS brain_conversations (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL,
    user_query              TEXT NOT NULL,
    ai_response             TEXT NOT NULL,
    referenced_email_ids    TEXT,
    created_at              TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_brain_conversations_user ON brain_conversations(user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS notifications (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL,
    title                   VARCHAR(512) NOT NULL,
    message                 TEXT NOT NULL,
    notification_type       VARCHAR(64) NOT NULL,
    related_email_id        BIGINT,
    is_read                 BOOLEAN DEFAULT FALSE,
    scheduled_at            TIMESTAMP,
    created_at              TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id, is_read);

CREATE TABLE IF NOT EXISTS followup_reminders (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL REFERENCES users(id),
    email_id                BIGINT REFERENCES emails(id),
    email_message_id        VARCHAR(255),
    sender_email            VARCHAR(512),
    subject                 VARCHAR(1024),
    reminder_time           TIMESTAMP,
    status                  VARCHAR(64),
    snoozed_until           TIMESTAMP,
    created_at              TIMESTAMP,
    updated_at              TIMESTAMP
);
