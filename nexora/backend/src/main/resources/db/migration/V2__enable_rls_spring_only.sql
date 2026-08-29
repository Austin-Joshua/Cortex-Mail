-- Harden public app tables for Spring-only access.
-- Do NOT alter flyway_schema_history here (locks the migration transaction).
-- RLS on + no policies for anon/authenticated = Data API denied.
-- Spring connects as the DB role and remains unaffected (no FORCE RLS).

ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.emails ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.email_attachments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.email_actions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.email_drafts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.email_templates ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.brain_conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.followup_reminders ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON TABLE public.users FROM anon, authenticated;
REVOKE ALL ON TABLE public.emails FROM anon, authenticated;
REVOKE ALL ON TABLE public.email_attachments FROM anon, authenticated;
REVOKE ALL ON TABLE public.email_actions FROM anon, authenticated;
REVOKE ALL ON TABLE public.email_drafts FROM anon, authenticated;
REVOKE ALL ON TABLE public.email_templates FROM anon, authenticated;
REVOKE ALL ON TABLE public.brain_conversations FROM anon, authenticated;
REVOKE ALL ON TABLE public.notifications FROM anon, authenticated;
REVOKE ALL ON TABLE public.followup_reminders FROM anon, authenticated;
