import React, { useEffect, useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { FileText, Search, X, Plus, RefreshCw } from 'lucide-react';
import { AppShell } from '../components/layout/AppShell';
import { EmailList } from '../components/email/EmailList';
import { EmailDetail } from '../components/email/EmailDetail';
import { emailApi } from '../api/emailApi';
import { draftsApi } from '../api/draftsApi';
import { templatesApi } from '../api/templatesApi';
import { queryKeys } from '../api/queryKeys';
import { useEmailStore } from '../store/emailStore';
import { useViewport } from '../hooks/useViewport';
import { Placeholder } from '../components/bento/Placeholder';

export const DraftsPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { isMobile } = useViewport();
  const { selectedEmail, setSelectedEmail } = useEmailStore();
  const [search, setSearch] = useState('');
  const [composing, setComposing] = useState(false);
  const [to, setTo] = useState('');
  const [cc, setCc] = useState('');
  const [subject, setSubject] = useState('');
  const [body, setBody] = useState('');
  const [saving, setSaving] = useState(false);
  const [composeStatus, setComposeStatus] = useState('');

  useEffect(() => {
    setSelectedEmail(null);
  }, [setSelectedEmail]);

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: queryKeys.emailDrafts(search),
    queryFn: () => emailApi.getDrafts({ search: search || undefined, page: 0, size: 100 }),
    staleTime: 60_000,
  });

  const { data: cortexDrafts = [] } = useQuery({
    queryKey: queryKeys.cortexDrafts,
    queryFn: draftsApi.list,
    staleTime: 15_000,
  });

  const { data: templates = [] } = useQuery({
    queryKey: queryKeys.emailTemplates,
    queryFn: templatesApi.list,
    staleTime: 60_000,
    enabled: composing,
  });

  const { data: syncStatus } = useQuery({
    queryKey: queryKeys.syncStatus,
    queryFn: emailApi.getSyncStatus,
    staleTime: 15_000,
  });

  const emails = data?.content ?? [];
  const total = data?.totalElements ?? 0;
  const gmailDrafts = syncStatus?.gmailCounts?.drafts ?? 0;
  const localDrafts = syncStatus?.localCounts?.drafts ?? total;
  const draftsStillLoading = gmailDrafts > localDrafts;
  const draftPulled = useRef(false);

  const pullDrafts = useMutation({
    mutationFn: emailApi.syncDrafts,
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['email-drafts'] }),
        queryClient.invalidateQueries({ queryKey: queryKeys.syncStatus }),
      ]);
    },
  });

  useEffect(() => {
    if (draftPulled.current || syncStatus == null || pullDrafts.isPending) return;
    if (!syncStatus.connected) return;
    const gmailKnowsDrafts = gmailDrafts > localDrafts;
    const labelsUnknown = !syncStatus.draftsAligned && localDrafts === 0;
    if (gmailKnowsDrafts || labelsUnknown) {
      draftPulled.current = true;
      pullDrafts.mutate();
    }
  }, [gmailDrafts, localDrafts, syncStatus, pullDrafts]);
  const showDetail = !!selectedEmail;
  const mobileDetailOnly = isMobile && showDetail;

  const handleSelect = async (email: { id: number; isRead: boolean }) => {
    setSelectedEmail(email as never);
    if (!email.isRead) {
      try {
        await emailApi.markRead(email.id);
        queryClient.invalidateQueries({ queryKey: queryKeys.emailDrafts() });
      } catch { /* ignore */ }
    }
  };

  const applyTemplate = async (id: number) => {
    const tpl = templates.find((t) => t.id === id);
    if (!tpl) return;
    setSubject(tpl.subject ?? subject);
    setBody(tpl.body ?? body);
    try {
      await templatesApi.recordUse(id);
      queryClient.invalidateQueries({ queryKey: queryKeys.emailTemplates });
    } catch { /* ignore */ }
  };

  const saveCortexDraft = async () => {
    setSaving(true);
    setComposeStatus('');
    try {
      await draftsApi.create({ to, cc, subject, body, draftStatus: 'DRAFT' });
      await queryClient.invalidateQueries({ queryKey: queryKeys.cortexDrafts });
      setTo('');
      setCc('');
      setSubject('');
      setBody('');
      setComposing(false);
      setComposeStatus('Draft saved in Cortex. Copy it into Gmail to send — Cortex does not send mail.');
    } catch {
      setComposeStatus('Could not save draft.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <AppShell
      noScroll
      flush
      title="Drafts"
      subtitle={
        gmailDrafts > 0 || total > 0 || cortexDrafts.length > 0
          ? `${localDrafts} stored · ${gmailDrafts} in Gmail · ${cortexDrafts.length} Cortex`
          : 'Gmail Drafts plus Cortex compose'
      }
      actions={
        <div style={{ display: 'flex', gap: 8 }}>
          <button
            type="button"
            className="vbtn vbtn-quiet"
            disabled={pullDrafts.isPending}
            onClick={() => pullDrafts.mutate()}
          >
            <RefreshCw size={16} /> {pullDrafts.isPending ? 'Pulling…' : 'Pull Gmail drafts'}
          </button>
          <button type="button" className="vbtn vbtn-quiet" onClick={() => setComposing((v) => !v)}>
            <Plus size={16} /> {composing ? 'Close compose' : 'New Cortex draft'}
          </button>
        </div>
      }
    >
      {composing && (
        <div className="settings-card" style={{ margin: '12px 16px 0' }}>
          <p className="settings-copy">Saved here only. Gmail stays the place you send from.</p>
          {templates.length > 0 && (
            <label className="v-meta" style={{ display: 'block', marginBottom: 8 }}>
              Template
              <select
                defaultValue=""
                onChange={(e) => e.target.value && void applyTemplate(Number(e.target.value))}
                style={{ display: 'block', width: '100%', marginTop: 4, height: 36 }}
              >
                <option value="">Choose a template…</option>
                {templates.map((t) => (
                  <option key={t.id} value={t.id}>{t.name}</option>
                ))}
              </select>
            </label>
          )}
          <input value={to} onChange={(e) => setTo(e.target.value)} placeholder="To" style={{ width: '100%', marginBottom: 8, height: 36, padding: '0 10px' }} />
          <input value={cc} onChange={(e) => setCc(e.target.value)} placeholder="Cc (optional)" style={{ width: '100%', marginBottom: 8, height: 36, padding: '0 10px' }} />
          <input value={subject} onChange={(e) => setSubject(e.target.value)} placeholder="Subject" style={{ width: '100%', marginBottom: 8, height: 36, padding: '0 10px' }} />
          <textarea value={body} onChange={(e) => setBody(e.target.value)} placeholder="Body" rows={6} style={{ width: '100%', marginBottom: 8, padding: 10, fontFamily: 'inherit' }} />
          <button type="button" className="vbtn vbtn-quiet" disabled={saving} onClick={() => void saveCortexDraft()}>
            {saving ? 'Saving…' : 'Save Cortex draft'}
          </button>
          {composeStatus && <p className="settings-status" role="status">{composeStatus}</p>}
        </div>
      )}

      {cortexDrafts.length > 0 && (
        <div style={{ padding: '8px 16px 0' }}>
          <p className="section-label">CORTEX DRAFTS</p>
          {cortexDrafts.map((d) => (
            <div key={d.id} className="stream-row" style={{ marginTop: 6 }}>
              <div style={{ minWidth: 0, flex: 1 }}>
                <div className="truncate" style={{ fontSize: 13, fontWeight: 700 }}>{d.subject || '(no subject)'}</div>
                <div className="v-meta truncate">{d.to || 'No recipient'}</div>
              </div>
              <button
                type="button"
                className="vbtn vbtn-bare"
                onClick={async () => {
                  await draftsApi.remove(d.id);
                  queryClient.invalidateQueries({ queryKey: queryKeys.cortexDrafts });
                }}
              >
                Delete
              </button>
            </div>
          ))}
        </div>
      )}

      <div className="mail-workspace" style={{ minHeight: 320 }}>
        {!mobileDetailOnly && (
          <div
            className="mail-workspace-list"
            style={{
              flex: showDetail && !isMobile ? '0 0 42%' : 1,
              borderRight: showDetail && !isMobile ? '1px solid var(--v-hairline)' : undefined,
            }}
          >
            <div style={{ padding: '10px 12px', borderBottom: '1px solid var(--v-hairline)', display: 'flex', gap: 8 }}>
              <div style={{ flex: 1, display: 'flex', alignItems: 'center', gap: 8, height: 38, padding: '0 12px', borderRadius: 999, background: 'var(--v-ground-2)', border: '1px solid var(--v-hairline)' }}>
                <Search size={14} style={{ color: 'var(--v-ink-3)' }} />
                <input
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder="Search Gmail drafts…"
                  style={{ flex: 1, border: 'none', outline: 'none', background: 'transparent', fontSize: 13, color: 'var(--v-ink)', fontFamily: 'inherit' }}
                />
                {search && (
                  <button type="button" className="vbtn vbtn-bare" style={{ width: 28, padding: 0 }} onClick={() => setSearch('')}>
                    <X size={14} />
                  </button>
                )}
              </div>
            </div>

            {pullDrafts.isError && (
              <p className="settings-status" style={{ padding: '8px 12px 0' }} role="status">
                Could not pull Gmail drafts. Try Pull Gmail drafts again.
              </p>
            )}
            {isError ? (
              <div style={{ padding: 24, textAlign: 'center' }}>
                <p className="v-body" style={{ marginBottom: 12 }}>Couldn’t load drafts.</p>
                <button type="button" className="vbtn vbtn-quiet" onClick={() => void refetch()}>Retry</button>
              </div>
            ) : emails.length === 0 && !isLoading ? (
              <Placeholder
                icon={<FileText size={26} />}
                headline={
                  pullDrafts.isPending || draftsStillLoading
                    ? 'Pulling Gmail drafts…'
                    : gmailDrafts > 0
                      ? `${gmailDrafts} drafts in Gmail are not stored yet`
                      : 'No drafts in Gmail'
                }
                body={
                  pullDrafts.isPending || draftsStillLoading
                    ? 'First inbox sync skips drafts so mail appears faster. This page pulls the Gmail Drafts folder on its own.'
                    : gmailDrafts > 0
                      ? 'Inbox sync stored your mail first. Tap Pull Gmail drafts to fetch the Drafts folder now.'
                      : 'Gmail Drafts is empty. Use New Cortex draft to stash text here, then copy it into Gmail to send. Cortex does not send mail.'
                }
                points={[
                  gmailDrafts > 0 ? `${gmailDrafts} on Gmail · ${localDrafts} stored here` : 'Gmail DRAFT label is empty',
                  'Cortex drafts stay in this app',
                  'Inbox sync does not wait on drafts',
                ]}
                action={
                  gmailDrafts > 0
                    ? { label: pullDrafts.isPending ? 'Pulling…' : 'Pull Gmail drafts', onClick: () => pullDrafts.mutate() }
                    : { label: 'Open inbox', onClick: () => navigate('/inbox') }
                }
              />
            ) : (
              <div className="v-scroll" style={{ flex: 1, overflowY: 'auto' }}>
                <EmailList emails={emails} isLoading={isLoading} onEmailSelect={handleSelect} />
              </div>
            )}
          </div>
        )}

        {showDetail && (
          <div className="mail-workspace-detail v-scroll">
            <EmailDetail
              emailId={selectedEmail.id}
              onClose={() => setSelectedEmail(null)}
            />
          </div>
        )}
      </div>
    </AppShell>
  );
};
