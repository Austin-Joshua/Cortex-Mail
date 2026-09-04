import React from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { Share2, Search, X } from 'lucide-react';
import { AppShell } from '../components/layout/AppShell';
import { EmailList } from '../components/email/EmailList';
import { EmailDetail } from '../components/email/EmailDetail';
import { emailApi } from '../api/emailApi';
import { queryKeys } from '../api/queryKeys';
import { useEmailStore } from '../store/emailStore';
import { useViewport } from '../hooks/useViewport';
import { Placeholder } from '../components/bento/Placeholder';

export const SharedPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { isMobile } = useViewport();
  const { selectedEmail, setSelectedEmail } = useEmailStore();
  const [search, setSearch] = React.useState('');

  React.useEffect(() => {
    setSelectedEmail(null);
  }, [setSelectedEmail]);

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: queryKeys.sharedEmails(search),
    queryFn: () => emailApi.getShared({ page: 0, size: 80 }),
    staleTime: 30_000,
  });

  const emails = React.useMemo(() => {
    const rows = data?.content ?? [];
    const q = search.trim().toLowerCase();
    if (!q) return rows;
    return rows.filter((e) =>
      `${e.subject ?? ''} ${e.senderName ?? ''} ${e.senderEmail ?? ''} ${e.recipientCc ?? ''}`
        .toLowerCase()
        .includes(q),
    );
  }, [data, search]);

  const showDetail = !!selectedEmail;
  const mobileDetailOnly = isMobile && showDetail;

  return (
    <AppShell
      noScroll
      flush
      title="Shared"
      subtitle="Mail with CC’d people, forums, or social labels from your Gmail"
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
            <div style={{ padding: '10px 12px', borderBottom: '1px solid var(--v-hairline)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, height: 38, padding: '0 12px', borderRadius: 999, background: 'var(--v-ground-2)', border: '1px solid var(--v-hairline)' }}>
                <Search size={14} style={{ color: 'var(--v-ink-3)' }} />
                <input
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder="Filter shared mail…"
                  style={{ flex: 1, border: 'none', outline: 'none', background: 'transparent', fontSize: 13, color: 'var(--v-ink)', fontFamily: 'inherit' }}
                />
                {search && (
                  <button type="button" className="vbtn vbtn-bare" style={{ width: 28, padding: 0 }} onClick={() => setSearch('')}>
                    <X size={14} />
                  </button>
                )}
              </div>
            </div>

            {isError ? (
              <div style={{ padding: 24, textAlign: 'center' }}>
                <p className="v-body" style={{ marginBottom: 12 }}>Couldn’t load shared mail.</p>
                <button type="button" className="vbtn vbtn-quiet" onClick={() => void refetch()}>Retry</button>
              </div>
            ) : emails.length === 0 && !isLoading ? (
              <Placeholder
                icon={<Share2 size={26} />}
                headline="No shared threads yet"
                body="This list is mail you CC’d others on, plus Gmail Forums and Social. Sync Gmail if it looks empty."
                points={['Uses CC and Gmail labels already stored', 'Not a separate sharing product', 'Updates after each sync']}
                action={{ label: 'Open inbox', onClick: () => navigate('/inbox') }}
              />
            ) : (
              <div className="v-scroll" style={{ flex: 1, overflowY: 'auto' }}>
                <EmailList
                  emails={emails}
                  isLoading={isLoading}
                  onEmailSelect={async (email) => {
                    setSelectedEmail(email);
                    if (!email.isRead) {
                      try {
                        await emailApi.markRead(email.id);
                        queryClient.invalidateQueries({ queryKey: queryKeys.sharedEmails() });
                      } catch { /* ignore */ }
                    }
                  }}
                />
              </div>
            )}
          </div>
        )}

        {showDetail && (
          <div className="mail-workspace-detail v-scroll">
            <EmailDetail emailId={selectedEmail.id} onClose={() => setSelectedEmail(null)} />
          </div>
        )}
      </div>
    </AppShell>
  );
};
