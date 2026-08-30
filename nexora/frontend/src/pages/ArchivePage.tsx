import React, { useEffect, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { Archive, Search, X } from 'lucide-react';
import { AppShell } from '../components/layout/AppShell';
import { EmailList } from '../components/email/EmailList';
import { EmailDetail } from '../components/email/EmailDetail';
import { emailApi } from '../api/emailApi';
import { queryKeys } from '../api/queryKeys';
import { useEmailStore } from '../store/emailStore';
import { useViewport } from '../hooks/useViewport';
import { Placeholder } from '../components/bento/Placeholder';

export const ArchivePage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { isMobile } = useViewport();
  const { selectedEmail, setSelectedEmail } = useEmailStore();
  const [search, setSearch] = useState('');

  useEffect(() => {
    setSelectedEmail(null);
  }, [setSelectedEmail]);

  const { data, isLoading } = useQuery({
    queryKey: queryKeys.emailArchived(search),
    queryFn: () => emailApi.getArchived({ search: search || undefined, page: 0, size: 100 }),
    staleTime: 60_000,
  });

  const emails = data?.content ?? [];
  const total = data?.totalElements ?? 0;
  const showDetail = !!selectedEmail;
  const mobileDetailOnly = isMobile && showDetail;

  const handleSelect = async (email: any) => {
    setSelectedEmail(email);
    if (!email.isRead) {
      try {
        await emailApi.markRead(email.id);
        queryClient.invalidateQueries({ queryKey: queryKeys.emailArchived() });
      } catch { /* ignore */ }
    }
  };

  return (
    <AppShell
      noScroll
      flush
      title="Archive"
      subtitle={total > 0 ? `${total} archived from Gmail` : 'Mail cleared from your inbox, still searchable'}
    >
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
                  placeholder="Search archive…"
                  style={{ flex: 1, border: 'none', outline: 'none', background: 'transparent', fontSize: 13, color: 'var(--v-ink)', fontFamily: 'inherit' }}
                />
                {search && (
                  <button type="button" className="vbtn vbtn-bare" style={{ width: 28, padding: 0 }} onClick={() => setSearch('')}>
                    <X size={14} />
                  </button>
                )}
              </div>
            </div>

            {emails.length === 0 && !isLoading ? (
              <Placeholder
                icon={<Archive size={26} />}
                tone="var(--v-ink-3)"
                headline="Archive is empty"
                body="Archived Gmail messages (removed from Inbox, not trash or spam) appear here after sync — with subject, sender, body, and any extracted details."
                points={['Synced from Gmail (not in Inbox)', 'Keeps category and summary', 'Fully searchable']}
                action={{ label: 'Open inbox', onClick: () => navigate('/inbox') }}
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
