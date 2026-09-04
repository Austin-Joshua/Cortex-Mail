import React, { useEffect, useMemo, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useInfiniteQuery, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCheck } from 'lucide-react';
import { AppShell } from '../components/layout/AppShell';
import { EmailList } from '../components/email/EmailList';
import { EmailDetail } from '../components/email/EmailDetail';
import { SenderView } from '../components/email/SenderView';
import { useEmailStore } from '../store/emailStore';
import { useAuthStore } from '../store/authStore';
import { emailApi } from '../api/emailApi';
import { queryKeys } from '../api/queryKeys';
import { useViewport } from '../hooks/useViewport';
import { useDebouncedValue } from '../hooks/useDebouncedValue';
import {
  getVisibleInboxDivisions,
  gmailViewCount,
  isGmailMailboxView,
  type InboxDivisionKey,
  type GmailMailboxView,
} from '../utils/inboxDivisions';
import type { Email, EmailCategory, EmailPage } from '../types/Email';

type ViewMode = EmailCategory | GmailMailboxView | 'ALL' | 'SENDERS';

const INBOX_PAGE_SIZE = 60;

function isCategoryView(view: ViewMode): view is EmailCategory {
  return view !== 'ALL' && view !== 'SENDERS' && !isGmailMailboxView(view);
}

function patchEmailReadInCache(old: unknown, emailId: number): unknown {
  if (!old || typeof old !== 'object') return old;
  const page = old as EmailPage & { pages?: EmailPage[] };
  if (Array.isArray(page.pages)) {
    return {
      ...page,
      pages: page.pages.map((p) => ({
        ...p,
        content: p.content.map((e) => (e.id === emailId ? { ...e, isRead: true } : e)),
      })),
    };
  }
  if (Array.isArray(page.content)) {
    return {
      ...page,
      content: page.content.map((e) => (e.id === emailId ? { ...e, isRead: true } : e)),
    };
  }
  return old;
}

export const InboxPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const urlEmailId = searchParams.get('emailId');
  const urlCategory = searchParams.get('category');
  const urlView = searchParams.get('view');
  const urlSearch = searchParams.get('search');
  const activeView: ViewMode = (urlCategory as ViewMode | null)
    ?? (urlView as ViewMode | null)
    ?? 'ALL';

  const { isMobile, isTablet } = useViewport();
  const { searchQuery, selectedEmail, setSelectedEmail, setSearchQuery } = useEmailStore();
  const debouncedSearch = useDebouncedValue(searchQuery, 400);
  const userRole = useAuthStore((s) => s.user?.userRole);
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [busy, setBusy] = useState(false);

  const categoryParam = isCategoryView(activeView) ? activeView : undefined;
  const viewParam = isGmailMailboxView(activeView) ? activeView : undefined;

  const {
    data: emailPages,
    isLoading: listLoading,
    isError: listError,
    refetch: refetchList,
    isFetchingNextPage,
    hasNextPage,
    fetchNextPage,
  } = useInfiniteQuery({
    queryKey: queryKeys.emailInbox(activeView, debouncedSearch),
    queryFn: ({ pageParam }) => emailApi.getEmails({
      page: pageParam,
      size: INBOX_PAGE_SIZE,
      category: categoryParam,
      view: viewParam,
      search: debouncedSearch || undefined,
    }),
    initialPageParam: 0,
    getNextPageParam: (last) => {
      if (last.last === true) return undefined;
      if (last.totalPages > 0 && last.number + 1 >= last.totalPages) return undefined;
      if (!last.content?.length) return undefined;
      return last.number + 1;
    },
    staleTime: 30_000,
  });

  const { data: categoryCounts = {} } = useQuery({
    queryKey: queryKeys.emailCategories,
    queryFn: emailApi.getCategoryCounts,
    staleTime: 30_000,
  });

  const { data: labelCounts } = useQuery({
    queryKey: queryKeys.gmailLabelCounts,
    queryFn: emailApi.getGmailLabelCounts,
    staleTime: 120_000,
  });

  const visibleDivisions = useMemo(
    () => getVisibleInboxDivisions(userRole, categoryCounts, labelCounts),
    [userRole, categoryCounts, labelCounts],
  );

  const displayedEmails = useMemo(
    () => emailPages?.pages.flatMap((p) => p.content) ?? [],
    [emailPages],
  );
  const listTotal = emailPages?.pages[0]?.totalElements ?? displayedEmails.length;
  const inboxTotal = categoryCounts
    ? Object.entries(categoryCounts).reduce((sum, [, n]) => sum + Number(n), 0)
    : listTotal;
  const inboxUnread = labelCounts?.INBOX?.messagesUnread
    ?? displayedEmails.filter((e) => !e.isRead).length;

  useEffect(() => {
    setSelectedIds(new Set());
  }, [activeView, debouncedSearch]);

  const tabCount = (key: InboxDivisionKey | 'ALL'): number => {
    if (key === 'ALL') {
      return debouncedSearch ? listTotal : inboxTotal;
    }
    if (isGmailMailboxView(key)) {
      return gmailViewCount(key, labelCounts);
    }
    if (debouncedSearch && activeView === key) {
      return listTotal;
    }
    return Number(categoryCounts[key] ?? 0);
  };

  const showDetail = !!(selectedEmail || urlEmailId);
  const mobileDetailOnly = isMobile && showDetail;

  useEffect(() => {
    if (urlSearch) setSearchQuery(urlSearch);
  }, [urlSearch, setSearchQuery]);

  useEffect(() => {
    if (urlEmailId && displayedEmails.length > 0) {
      const found = displayedEmails.find((e) => e.id === parseInt(urlEmailId, 10));
      if (found) setSelectedEmail(found);
    }
  }, [urlEmailId, displayedEmails, setSelectedEmail]);

  const inboxHref = (key: ViewMode) => {
    if (key === 'ALL') return '/inbox';
    if (key === 'SENDERS') return '/inbox?category=SENDERS';
    if (isGmailMailboxView(key)) return `/inbox?view=${key}`;
    return `/inbox?category=${key}`;
  };

  const handleTabClick = (key: ViewMode) => {
    setSelectedEmail(null);
    navigate(inboxHref(key), { replace: true });
  };

  const invalidateMailbox = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: queryKeys.emails }),
      queryClient.invalidateQueries({ queryKey: queryKeys.gmailLabelCounts }),
      queryClient.invalidateQueries({ queryKey: queryKeys.dashboardSummary }),
      queryClient.invalidateQueries({ queryKey: queryKeys.emailCategories }),
      queryClient.invalidateQueries({ queryKey: queryKeys.syncStatus }),
    ]);
  };

  const handleEmailSelect = async (email: Email) => {
    setSelectedEmail(email);
    if (email.isRead) return;

    setSelectedEmail({ ...email, isRead: true });
    queryClient.setQueriesData({ queryKey: queryKeys.emails }, (old) => patchEmailReadInCache(old, email.id));

    try {
      await emailApi.markRead(email.id);
      queryClient.invalidateQueries({ queryKey: queryKeys.gmailLabelCounts });
      queryClient.invalidateQueries({ queryKey: queryKeys.dashboardSummary });
    } catch {
      setSelectedEmail(email);
      queryClient.invalidateQueries({ queryKey: queryKeys.emails });
    }
  };

  const handleMarkAllRead = async () => {
    setBusy(true);
    try {
      await emailApi.markAllRead();
      setSelectedIds(new Set());
      await invalidateMailbox();
    } finally {
      setBusy(false);
    }
  };

  const handleBulk = async (action: 'READ' | 'UNREAD' | 'STAR' | 'UNSTAR' | 'ARCHIVE' | 'TRASH') => {
    const ids = [...selectedIds];
    if (ids.length === 0) return;
    setBusy(true);
    try {
      await emailApi.bulk(ids, action);
      setSelectedIds(new Set());
      await invalidateMailbox();
    } finally {
      setBusy(false);
    }
  };

  return (
    <AppShell
      noScroll
      flush
      title="Inbox"
      subtitle="Gmail tabs plus groups Cortex learned from your mail"
      actions={
        inboxUnread > 0 ? (
          <button type="button" className="vbtn vbtn-quiet" disabled={busy} onClick={() => void handleMarkAllRead()}>
            <CheckCheck size={16} /> Mark all as read
          </button>
        ) : undefined
      }
    >
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

            {visibleDivisions.map(({ key, label, kind }) => {
              const isActive = activeView === key;
              const count = tabCount(key);

              return (
                <button
                  key={key}
                  type="button"
                  onClick={() => handleTabClick(key)}
                  className={`gmail-tab${kind === 'cortex' ? ' cortex-tab' : ''}${isActive ? ' active' : ''}`}
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
                  {listError ? (
                    <div style={{ padding: 24, textAlign: 'center' }}>
                      <p className="v-body" style={{ marginBottom: 12 }}>Couldn’t load inbox.</p>
                      <button type="button" className="vbtn vbtn-quiet" onClick={() => void refetchList()}>
                        Retry
                      </button>
                    </div>
                  ) : (
                    <EmailList
                      emails={displayedEmails}
                      isLoading={listLoading}
                      onEmailSelect={handleEmailSelect}
                      hasMore={Boolean(hasNextPage)}
                      isLoadingMore={isFetchingNextPage}
                      onLoadMore={() => { void fetchNextPage(); }}
                      selectable
                      selectedIds={selectedIds}
                      onToggleSelect={(id) => {
                        setSelectedIds((prev) => {
                          const next = new Set(prev);
                          if (next.has(id)) next.delete(id);
                          else next.add(id);
                          return next;
                        });
                      }}
                      onSelectAll={() => setSelectedIds(new Set(displayedEmails.map((e) => e.id)))}
                      onClearSelection={() => setSelectedIds(new Set())}
                      onBulkAction={(action) => void handleBulk(action)}
                      onMarkAllRead={() => void handleMarkAllRead()}
                      unreadCount={inboxUnread}
                      busy={busy}
                    />
                  )}
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
                      navigate(inboxHref(activeView), { replace: true });
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
