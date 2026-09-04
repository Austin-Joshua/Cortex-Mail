import React, { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { AppShell } from '../components/layout/AppShell';
import { useAuth } from '../hooks/useAuth';
import { useAuthStore } from '../store/authStore';
import { emailApi } from '../api/emailApi';
import { settingsApi } from '../api/settingsApi';
import { templatesApi } from '../api/templatesApi';
import { queryKeys } from '../api/queryKeys';
import { Calendar, Link2, LogOut, RefreshCw, Shield, Sparkles, User, FileText } from 'lucide-react';

const SECURITY_POINTS = [
  'Mailbox changes (read, star, archive, trash) only run when you click them',
  'Gmail tokens encrypted at rest (AES-GCM)',
  'Sessions expire automatically; logout revokes the session token',
  'Your synced mail stays in your workspace — not sold or used for ads',
];

export const SettingsPage: React.FC = () => {
  const { user } = useAuthStore();
  const { updateProfile, handleLogout, handleGoogleLogin } = useAuth();
  const queryClient = useQueryClient();
  const [calendarSyncEnabled, setCalendarSyncEnabled] = useState(user?.calendarSyncEnabled ?? true);
  const [reclassifying, setReclassifying] = useState(false);
  const [status, setStatus] = useState('');
  const [tplName, setTplName] = useState('');
  const [tplSubject, setTplSubject] = useState('');
  const [tplBody, setTplBody] = useState('');

  const { data: gemini } = useQuery({
    queryKey: queryKeys.geminiStatus,
    queryFn: settingsApi.getGeminiStatus,
    staleTime: 60_000,
  });

  const { data: templates = [] } = useQuery({
    queryKey: queryKeys.emailTemplates,
    queryFn: templatesApi.list,
    staleTime: 30_000,
  });

  const initials = user?.name
    ? user.name.split(/\s+/).filter(Boolean).map((n) => n[0]).join('').slice(0, 2).toUpperCase()
    : 'CM';

  const handleCalendarToggle = async (val: boolean) => {
    setCalendarSyncEnabled(val);
    try {
      await updateProfile({ calendarSyncEnabled: val });
      setStatus(val ? 'Calendar export enabled.' : 'Calendar export turned off.');
    } catch {
      setCalendarSyncEnabled(!val);
      setStatus('Could not update calendar preference.');
    }
  };

  const handleReanalyze = async () => {
    setReclassifying(true);
    setStatus('Re-analyzing your inbox from message content…');
    try {
      const result = await emailApi.classifyInbox({ force: true });
      await queryClient.invalidateQueries({ queryKey: queryKeys.emails });
      await queryClient.invalidateQueries({ queryKey: queryKeys.emailCategories });
      await queryClient.invalidateQueries({ queryKey: queryKeys.dashboardSummary });
      setStatus(result.message || 'Re-analysis started. Groups update as mail is classified.');
    } catch {
      setStatus('Re-analysis failed. Check that the backend is running.');
    } finally {
      setReclassifying(false);
    }
  };

  return (
    <AppShell title="Settings" subtitle="Account, intelligence, and privacy">
      <div className="settings-stack">
        <section className="settings-card">
          <div className="settings-card-head">
            <User size={16} />
            <h2>Profile</h2>
          </div>
          <div className="settings-profile-row">
            <div className="settings-avatar" aria-hidden>
              {user?.profilePictureUrl ? (
                <img src={user.profilePictureUrl} alt="" />
              ) : (
                <span>{initials}</span>
              )}
            </div>
            <div style={{ minWidth: 0 }}>
              <p className="settings-name">{user?.name || 'Signed in'}</p>
              <p className="settings-email">{user?.email}</p>
              <p className="settings-hint">
                Classification is personalized from your mailbox — not a student, manager, or job-title profile.
              </p>
            </div>
          </div>
        </section>

        <section className="settings-card">
          <div className="settings-card-head">
            <Link2 size={16} />
            <h2>Gmail connection</h2>
          </div>
          <p className="settings-copy">
            If sync stops or tokens expire, reconnect Google to refresh access without changing your Cortex account.
          </p>
          <button
            type="button"
            className="vbtn vbtn-quiet"
            style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}
            onClick={() => handleGoogleLogin()}
          >
            <Link2 size={15} />
            Reconnect Gmail
          </button>
        </section>

        <section className="settings-card">
          <div className="settings-card-head">
            <RefreshCw size={16} />
            <h2>Inbox intelligence</h2>
          </div>
          <p className="settings-copy">
            Cortex groups mail by content, senders, and Gmail signals for this account. Re-run analysis anytime after a big sync — existing groups stay visible while labels refresh.
          </p>
          <button
            type="button"
            className="vbtn vbtn-quiet"
            style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}
            disabled={reclassifying}
            onClick={() => void handleReanalyze()}
          >
            <RefreshCw size={15} className={reclassifying ? 'animate-spin' : undefined} />
            {reclassifying ? 'Re-analyzing…' : 'Re-analyze inbox'}
          </button>
        </section>

        <section className="settings-card">
          <div className="settings-card-head">
            <Sparkles size={16} />
            <h2>AI enrichment</h2>
          </div>
          <p className="settings-copy">
            {gemini?.configured
              ? 'Gemini is on for this workspace. Inbox groups still run from Gmail labels and content first; Gemini only refines in the background.'
              : 'No Gemini key is configured. Grouping and Cortex Score use local rules and Gmail labels only.'}
          </p>
          <p className="v-meta">Mode: {gemini?.mode === 'gemini' ? 'Gemini + rules' : 'Rules only'}</p>
        </section>

        <section className="settings-card">
          <div className="settings-card-head">
            <FileText size={16} />
            <h2>Reply templates</h2>
          </div>
          <p className="settings-copy">Used from Drafts → New Cortex draft. Stored per account.</p>
          <input value={tplName} onChange={(e) => setTplName(e.target.value)} placeholder="Template name" style={{ width: '100%', marginBottom: 8, height: 36, padding: '0 10px' }} />
          <input value={tplSubject} onChange={(e) => setTplSubject(e.target.value)} placeholder="Subject" style={{ width: '100%', marginBottom: 8, height: 36, padding: '0 10px' }} />
          <textarea value={tplBody} onChange={(e) => setTplBody(e.target.value)} placeholder="Body" rows={4} style={{ width: '100%', marginBottom: 8, padding: 10, fontFamily: 'inherit' }} />
          <button
            type="button"
            className="vbtn vbtn-quiet"
            disabled={!tplName.trim()}
            onClick={async () => {
              try {
                await templatesApi.create({ name: tplName.trim(), subject: tplSubject, body: tplBody });
                setTplName('');
                setTplSubject('');
                setTplBody('');
                queryClient.invalidateQueries({ queryKey: queryKeys.emailTemplates });
                setStatus('Template saved.');
              } catch {
                setStatus('Could not save template.');
              }
            }}
          >
            Save template
          </button>
          {templates.map((t) => (
            <div key={t.id} className="stream-row" style={{ marginTop: 8 }}>
              <div style={{ minWidth: 0, flex: 1 }}>
                <div className="truncate" style={{ fontWeight: 700, fontSize: 13 }}>{t.name}</div>
                <div className="v-meta truncate">{t.subject || 'No subject'}</div>
              </div>
              <button
                type="button"
                className="vbtn vbtn-bare"
                onClick={async () => {
                  await templatesApi.remove(t.id);
                  queryClient.invalidateQueries({ queryKey: queryKeys.emailTemplates });
                }}
              >
                Delete
              </button>
            </div>
          ))}
        </section>

        <section className="settings-card">
          <div className="settings-card-head">
            <Calendar size={16} />
            <h2>Calendar</h2>
          </div>
          <label className="settings-toggle-row" htmlFor="calendar-sync-toggle">
            <div>
              <div className="settings-toggle-title">Add detected deadlines to Google Calendar</div>
              <p className="settings-copy" style={{ margin: '4px 0 0' }}>
                Only when a deadline is clearly written in the email.
              </p>
            </div>
            <input
              id="calendar-sync-toggle"
              type="checkbox"
              checked={calendarSyncEnabled}
              onChange={(e) => void handleCalendarToggle(e.target.checked)}
            />
          </label>
        </section>

        <section className="settings-card">
          <div className="settings-card-head">
            <Shield size={16} />
            <h2>Privacy</h2>
          </div>
          <ul className="settings-list">
            {SECURITY_POINTS.map((pt) => (
              <li key={pt}>{pt}</li>
            ))}
          </ul>
          <button
            type="button"
            onClick={() => void handleLogout()}
            className="vbtn vbtn-quiet"
            style={{ color: 'var(--v-critical)', marginTop: 8 }}
          >
            <LogOut size={14} /> Revoke access &amp; log out
          </button>
        </section>

        {status && <p className="settings-status" role="status">{status}</p>}
      </div>
    </AppShell>
  );
};
