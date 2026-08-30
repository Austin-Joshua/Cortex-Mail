import React, { useEffect, useMemo, useRef } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { AppShell } from '../components/layout/AppShell';
import { EmailList } from '../components/email/EmailList';
import { EmailDetail } from '../components/email/EmailDetail';
import { SenderView } from '../components/email/SenderView';
import { useEmailStore } from '../store/emailStore';
import { useAuthStore } from '../store/authStore';
import { emailApi } from '../api/emailApi';
import { useViewport } from '../hooks/useViewport';
import {
  getVisibleInboxDivisions,
  type InboxDivisionKey,
} from '../utils/inboxDivisions';
import type { Email, EmailCategory } from '../types/Email';

type ViewMode = EmailCategory | 'ALL' | 'SENDERS';

const INBOX_FETCH_SIZE = 500;

function isCategoryView(view: ViewMode): view is EmailCategory {
  return view !== 'ALL' && view !== 'SENDERS';
}

export const InboxPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const urlEmailId = searchParams.get('emailId');
  const urlCategory = searchParams.get('category') as ViewMode | null;
  const activeView: ViewMode = urlCategory ?? 'ALL';

  const { isMobile, isTablet } = useViewport();
  const { searchQuery, selectedEmail, setSelectedEmail } = useEmailStore();
  const userRole = useAuthStore((s) => s.user?.userRole);

  const categoryParam = isCategoryView(activeView) ? activeView : undefined;

  const { data: emailPage, isLoading: listLoading } = useQuery({
    queryKey: ['emails', 'inbox', activeView, searchQuery],
    queryFn: () => emailApi.getEmails({
      page: 0,
      size: INBOX_FETCH_SIZE,
      category: categoryParam,
      search: searchQuery || undefined,
    }),
    staleTime: 30_000,
  });

  const { data: categoryCounts = {} } = useQuery({
    queryKey: ['email-categories'],
    queryFn: emailApi.getCategoryCounts,
    staleTime: 30_000,
  });

  const { data: labelCounts } = useQuery({
    queryKey: ['gmail-label-counts'],
    queryFn: emailApi.getGmailLabelCounts,
    staleTime: 120_000,
  });

  const visibleDivisions = useMemo(
    () => getVisibleInboxDivisions(userRole, categoryCounts),
    [userRole, categoryCounts],
  );

  const displayedEmails = emailPage?.content ?? [];
  const listTotal = emailPage?.totalElements ?? displayedEmails.length;
  const inboxTotal = categoryCounts
    ? Object.entries(categoryCounts).reduce((sum, [, n]) => sum + Number(n), 0)
    : listTotal;
  const inboxUnread = labelCounts?.INBOX?.messagesUnread
    ?? displayedEmails.filter((e) => !e.isRead).length;

  const tabCount = (key: InboxDivisionKey | 'ALL'): number => {
    if (key === 'ALL') {
      return searchQuery ? listTotal : inboxTotal;
    }
    if (searchQuery && activeView === key) {
      return listTotal;
    }
    return Number(categoryCounts[key] ?? 0);
  };

  const showDetail = !!(selectedEmail || urlEmailId);
  const mobileDetailOnly = isMobile && showDetail;

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
    if (urlEmailId && displayedEmails.length > 0) {
      const found = displayedEmails.find((e) => e.id === parseInt(urlEmailId, 10));
      if (found) setSelectedEmail(found);
    }
  }, [urlEmailId, displayedEmails, setSelectedEmail]);

  const handleTabClick = (key: ViewMode) => {
    setSelectedEmail(null);
    if (key === 'ALL') {
      navigate('/inbox', { replace: true });
    } else if (key !== 'SENDERS') {
      navigate(`/inbox?category=${key}`, { replace: true });
    } else {
      navigate('/inbox?category=SENDERS', { replace: true });
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
        queryClient.invalidateQueries({ queryKey: ['email-categories'] });
      } catch { /* ignore */ }
    }
  };

  return (
    <AppShell noScroll flush>
      <div
        className="mail-workspace"
        style={{
          flexDirection: 'column',
          border: 'none',
          boxShadow: 'none',
          borderRadius: 0,
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
              className={`gmail-tab${activeView === 'ALL' ? ' active' : ''}`}
              style={{ flexShrink: 0 }}
            >
              Inbox
              {inboxTotal > 0 && (
                <span
                  style={{
                    fontSize: 11,
                    color: activeView === 'ALL' ? 'var(--color-text-secondary)' : 'var(--color-text-muted)',
                    fontWeight: 700,
                  }}
                >
                  {inboxUnread > 0 ? `${inboxUnread} unread · ${tabCount('ALL')}` : tabCount('ALL')}
                </span>
              )}
            </button>

            {visibleDivisions.map(({ key, label }) => {
              const isActive = activeView === key;
              const count = tabCount(key);

              return (
                <button
                  key={key}
                  type="button"
                  onClick={() => handleTabClick(key)}
                  className={`gmail-tab cortex-tab${isActive ? ' active' : ''}`}
                >
                  {label}
                  {key !== 'SENDERS' && count > 0 && (
                    <span
                      style={{
                        fontSize: 11,
                        color: isActive ? 'var(--color-cortex-light)' : 'var(--color-text-muted)',
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
                    isLoading={listLoading}
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
                      navigate(activeView === 'ALL' ? '/inbox' : `/inbox?category=${activeView}`, { replace: true });
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
