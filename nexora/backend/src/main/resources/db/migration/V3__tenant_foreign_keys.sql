-- Harden tenant and parent FKs. Clean orphans first so validate/migrate can succeed.

DELETE FROM email_actions
 WHERE user_id NOT IN (SELECT id FROM users)
    OR email_id NOT IN (SELECT id FROM emails);

DELETE FROM brain_conversations
 WHERE user_id NOT IN (SELECT id FROM users);

DELETE FROM notifications
 WHERE user_id NOT IN (SELECT id FROM users);

ALTER TABLE email_actions
    DROP CONSTRAINT IF EXISTS email_actions_email_id_fkey;

ALTER TABLE email_actions
    ADD CONSTRAINT fk_email_actions_email
        FOREIGN KEY (email_id) REFERENCES emails(id) ON DELETE CASCADE;

ALTER TABLE email_actions
    ADD CONSTRAINT fk_email_actions_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE brain_conversations
    ADD CONSTRAINT fk_brain_conversations_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE notifications
    ADD CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
