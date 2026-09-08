-- Durable onboarding flag (existing users treated as complete).
ALTER TABLE users ADD COLUMN IF NOT EXISTS onboarding_complete BOOLEAN DEFAULT TRUE;
