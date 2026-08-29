import React, { useEffect, useMemo, useRef, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { AppShell } from '../components/layout/AppShell';
import { EmailList } from '../components/email/EmailList';
import { EmailDetail } from '../components/email/EmailDetail';
import { SenderView } from '../components/email/SenderView';
import { useEmailStore } from '../store/emailStore';
import { emailApi } from '../api/emailApi';
import { useViewport } from '../hooks/useViewport';
import type { Email, EmailCategory } from '../types/Email';

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

const INBOX_FETCH_SIZE = 200;

export const InboxPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const urlEmailId = searchParams.get('emailId');
  const urlCategory = searchParams.get('category') as ViewMode | null;

  const [activeView, setActiveView] = useState<ViewMode>(urlCategory ?? 'ALL');

  const { isMobile, isTablet } = useViewport();
  const { searchQuery, selectedEmail, setSelectedEmail } = useEmailStore();

  const { data: inboxPage, isLoading: inboxLoading } = useQuery({
    queryKey: ['emails', 'inbox-all', searchQuery],
    queryFn: () => emailApi.getEmails({
      page: 0,
      size: INBOX_FETCH_SIZE,
      search: searchQuery || undefined,
    }),
    staleTime: 45_000,
  });

  const { data: categoryCounts = {} } = useQuery({
    queryKey: ['email-categories'],
    queryFn: emailApi.getCategoryCounts,
    staleTime: 60_000,
  });

  const { data: labelCounts } = useQuery({
    queryKey: ['gmail-label-counts'],
    queryFn: emailApi.getGmailLabelCounts,
    staleTime: 120_000,
  });

  const allEmails = inboxPage?.content ?? [];
  const totalElements = inboxPage?.totalElements ?? allEmails.length;
  const inboxUnread = labelCounts?.INBOX?.messagesUnread ?? allEmails.filter((e) => !e.isRead).length;

  const displayedEmails = useMemo(() => {
    if (activeView === 'SENDERS') return [];
    if (activeView === 'ALL') return allEmails;
    return allEmails.filter((e) => e.category === activeView);
  }, [allEmails, activeView]);

  const showDetail = !!(selectedEmail || urlEmailId);
  const mobileDetailOnly = isMobile && showDetail;

  useEffect(() => {
    if (urlCategory && TABS.some((t) => t.key === urlCategory)) {
      setActiveView(urlCategory);
    } else {
      setActiveView('ALL');
    }
  }, [urlCategory]);

  const classifyOnce = useRef(false);

  useEffect(() => {
    const unclassified = Number(categoryCounts.UNCATEGORIZED ?? 0);
    if (unclassified <= 0 || classifyOnce.current) return;
    classifyOnce.current = true;
    emailApi.classifyInbox()
      .then(() => {
        queryClient.invalidateQueries({ queryKey: ['emails'] });
        queryClient.invalidateQueries({ queryKey: ['email-categories'] });
      })
      .catch(() => { classifyOnce.current = false; });
  }, [categoryCounts.UNCATEGORIZED, queryClient]);

  useEffect(() => {
    if (urlEmailId && allEmails.length > 0) {
      const found = allEmails.find((e) => e.id === parseInt(urlEmailId, 10));
      if (found) setSelectedEmail(found);
    }
  }, [urlEmailId, allEmails, setSelectedEmail]);

  const handleTabClick = (key: ViewMode) => {
    setActiveView(key);
    setSelectedEmail(null);
    if (key === 'ALL') {
      navigate('/inbox', { replace: true });
    } else if (key !== 'SENDERS') {
      navigate(`/inbox?category=${key}`, { replace: true });
    }
  };

  const handleEmailSelect = async (email: Email) => {
    setSelectedEmail(email);
    if (!email.isRead) {
      try {
        await emailApi.markRead(email.id);
        queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
        queryClient.invalidateQueries({ queryKey: ['gmail-label-counts'] });
        queryClient.invalidateQueries({ queryKey: ['emails'] });
      } catch { /* ignore */ }
    }
  };

  return (
    <AppShell noScroll flush>
      <div
        style={{
          display: 'flex',
          flex: 1,
          minHeight: 0,
          flexDirection: 'column',
          background: 'var(--bg)',
        }}
      >
        {!mobileDetailOnly && (
          <div
            className="inbox-tab-rail"
            style={{
              borderBottom: '1px solid var(--border)',
              background: 'var(--bg)',
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

        <div style={{ flex: 1, minHeight: 0, overflow: 'hidden' }}>
          {activeView === 'SENDERS' ? (
            <SenderView />
          ) : (
            <div style={{ display: 'flex', height: '100%', minHeight: 0, overflow: 'hidden' }}>
              {!mobileDetailOnly && (
                <div
                  className="v-scroll"
                  style={{
                    width: showDetail && !isMobile ? (isTablet ? '44%' : '40%') : '100%',
                    minWidth: showDetail && !isMobile ? (isTablet ? 280 : 360) : undefined,
                    maxWidth: showDetail && !isMobile ? (isTablet ? 420 : undefined) : undefined,
                    flexShrink: 0,
                    borderRight: showDetail && !isMobile ? '1px solid var(--border)' : 'none',
                    overflowY: 'auto',
                    minHeight: 0,
                    background: 'var(--bg)',
                  }}
                >
                  <EmailList
                    emails={displayedEmails}
                    isLoading={inboxLoading}
                    onEmailSelect={handleEmailSelect}
                  />
                </div>
              )}

              {showDetail && (
                <div
                  className="v-scroll"
                  style={{
                    flex: 1,
                    width: mobileDetailOnly ? '100%' : undefined,
                    overflowY: 'auto',
                    minHeight: 0,
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
