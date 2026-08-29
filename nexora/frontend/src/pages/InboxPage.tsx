import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { AppShell } from '../components/layout/AppShell';
import { EmailList } from '../components/email/EmailList';
import { EmailDetail } from '../components/email/EmailDetail';
import { SenderView } from '../components/email/SenderView';
import { useEmails } from '../hooks/useEmails';
import { useEmailStore } from '../store/emailStore';
import { emailApi } from '../api/emailApi';
import { useQueryClient } from '@tanstack/react-query';
import { useViewport } from '../hooks/useViewport';
import type { EmailCategory } from '../types/Email';

type ViewMode = EmailCategory | 'ALL' | 'SENDERS';

const TABS: Array<{ key: ViewMode; label: string }> = [
  { key: 'ALL',          label: 'Primary' },
  { key: 'SENDERS',      label: 'Senders' },
  { key: 'ASSIGNMENT',   label: 'Assignments' },
  { key: 'HACKATHON',    label: 'Hackathons' },
  { key: 'PLACEMENT',    label: 'Placement' },
  { key: 'ATTENDANCE',   label: 'Attendance' },
  { key: 'MEETING',      label: 'Meetings' },
  { key: 'ANNOUNCEMENT', label: 'Announcements' },
  { key: 'RESEARCH',     label: 'Research' },
  { key: 'PERSONAL',     label: 'Personal' },
];

export const InboxPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const urlEmailId = searchParams.get('emailId');
  const urlCategory = searchParams.get('category') as ViewMode | null;

  const [activeView, setActiveView] = useState<ViewMode>(urlCategory ?? 'ALL');

  const { isMobile, isTablet } = useViewport();
  const { setActiveCategory, selectedEmail, setSelectedEmail } = useEmailStore();
  const { emails, isLoading, categoryCounts, totalElements, inboxUnread } = useEmails(0, 80);
  const showDetail = !!(selectedEmail || urlEmailId);
  const mobileDetailOnly = isMobile && showDetail;

  useEffect(() => {
    if (urlCategory && TABS.some((t) => t.key === urlCategory)) {
      setActiveView(urlCategory);
      setActiveCategory(urlCategory as EmailCategory | 'ALL');
    } else {
      setActiveView('ALL');
      setActiveCategory('ALL');
    }
  }, [urlCategory, setActiveCategory]);

  useEffect(() => {
    if (urlEmailId && emails.length > 0) {
      const found = emails.find((e) => e.id === parseInt(urlEmailId, 10));
      if (found) setSelectedEmail(found);
    }
  }, [urlEmailId, emails, setSelectedEmail]);

  const handleTabClick = (key: ViewMode) => {
    setActiveView(key);
    if (key !== 'SENDERS') {
      setActiveCategory(key as EmailCategory | 'ALL');
    }
    setSelectedEmail(null);
    if (key === 'ALL') {
      navigate('/inbox', { replace: true });
    } else if (key !== 'SENDERS') {
      navigate(`/inbox?category=${key}`, { replace: true });
    }
  };

  const handleEmailSelect = async (email: { id: number; isRead?: boolean }) => {
    setSelectedEmail(email as any);
    if (!email.isRead) {
      try {
        await emailApi.markRead(email.id);
        queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
        queryClient.invalidateQueries({ queryKey: ['gmail-label-counts'] });
      } catch { /* ignore */ }
    }
  };

  return (
    <AppShell noScroll>
      <div style={{ display: 'flex', height: '100%', overflow: 'hidden', flexDirection: 'column', background: 'var(--bg)' }}>
        {!mobileDetailOnly && (
          <div
            style={{
              flexShrink: 0,
              borderBottom: '1px solid var(--border)',
              background: 'var(--bg)',
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              paddingRight: isMobile ? 8 : 16,
              overflowX: 'auto',
            }}
          >
            <button
              type="button"
              onClick={() => handleTabClick('ALL')}
              className="vbtn vbtn-bare"
              style={{
                padding: '0 12px',
                height: 44,
                whiteSpace: 'nowrap',
                fontWeight: 700,
                fontSize: 12,
                color: activeView === 'ALL' ? 'var(--v-signal)' : 'var(--v-ink-2)',
                flexShrink: 0,
              }}
            >
              {inboxUnread > 0 ? `${inboxUnread} unread` : 'Inbox'}
              {totalElements > 0 ? ` · ${totalElements}` : ''}
            </button>

            {TABS.map(({ key, label }) => {
              const isActive = activeView === key;
              const count = key !== 'ALL' && key !== 'SENDERS'
                ? (categoryCounts[key as string] ?? 0)
                : undefined;

              return (
                <button
                  key={key}
                  type="button"
                  onClick={() => handleTabClick(key)}
                  className={`gmail-tab${isActive ? ' active' : ''}`}
                >
                  {label}
                  {count !== undefined && count > 0 && (
                    <span
                      style={{
                        fontSize: 11,
                        color: isActive ? 'var(--accent)' : 'var(--text-3)',
                        fontWeight: 700,
                      }}
                    >
                      {count}
                    </span>
                  )}
                </button>
              );
            })}
          </div>
        )}

        <div style={{ flex: 1, overflow: 'hidden' }}>
          {activeView === 'SENDERS' ? (
            <SenderView />
          ) : (
            <div style={{ display: 'flex', height: '100%', overflow: 'hidden' }}>
              {!mobileDetailOnly && (
                <div
                  style={{
                    width: showDetail && !isMobile ? (isTablet ? '44%' : '40%') : '100%',
                    minWidth: showDetail && !isMobile ? (isTablet ? 280 : 360) : undefined,
                    maxWidth: showDetail && !isMobile ? (isTablet ? 420 : undefined) : undefined,
                    flexShrink: 0,
                    borderRight: showDetail && !isMobile ? '1px solid var(--border)' : 'none',
                    overflowY: 'auto',
                    background: 'var(--bg)',
                  }}
                >
                  <EmailList
                    emails={emails}
                    isLoading={isLoading}
                    onEmailSelect={handleEmailSelect}
                  />
                </div>
              )}

              {showDetail && (
                <div
                  style={{
                    flex: 1,
                    width: mobileDetailOnly ? '100%' : undefined,
                    overflow: 'hidden',
                    minWidth: 0,
                  }}
                >
                  <EmailDetail
                    emailId={selectedEmail ? selectedEmail.id : parseInt(urlEmailId!, 10)}
                    onClose={() => {
                      setSelectedEmail(null);
                      navigate('/inbox', { replace: true });
                    }}
                  />
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </AppShell>
  );
};
