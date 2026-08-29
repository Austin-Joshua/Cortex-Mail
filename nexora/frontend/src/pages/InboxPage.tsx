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
import { Eye, EyeOff, Search, X } from 'lucide-react';

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
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  const { isMobile, isTablet } = useViewport();
  const { setActiveCategory, selectedEmail, setSelectedEmail } = useEmailStore();
  const { emails, isLoading, categoryCounts, totalElements, inboxUnread } = useEmails(0, 500);
  const showDetail = !!(selectedEmail || urlEmailId);
  const mobileDetailOnly = isMobile && showDetail;

  useEffect(() => {
    if (urlCategory && TABS.some(t => t.key === urlCategory)) {
      setActiveView(urlCategory);
      setActiveCategory(urlCategory as EmailCategory | 'ALL');
    }
  }, [urlCategory]);

  useEffect(() => {
    if (urlEmailId && emails.length > 0) {
      const found = emails.find(e => e.id === parseInt(urlEmailId));
      if (found) setSelectedEmail(found);
    }
  }, [urlEmailId, emails]);

  const displayedEmails = emails.filter(email => {
    const matchesUnread = !unreadOnly || !email.isRead;
    if (!searchQuery.trim()) return matchesUnread;

    const query = searchQuery.toLowerCase();
    const matchesSearch =
      (email.senderName?.toLowerCase().includes(query)) ||
      (email.subject?.toLowerCase().includes(query)) ||
      (email.bodySnippet?.toLowerCase().includes(query)) ||
      (email.senderEmail?.toLowerCase().includes(query));

    return matchesUnread && matchesSearch;
  });

  const handleTabClick = (key: ViewMode) => {
    setActiveView(key);
    if (key !== 'SENDERS') {
      setActiveCategory(key as EmailCategory | 'ALL');
    }
    setSelectedEmail(null);
  };

  const handleEmailSelect = async (email: any) => {
    setSelectedEmail(email);
    if (!email.isRead) {
      try {
        await emailApi.markRead(email.id);
        queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
      } catch {}
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
              justifyContent: 'space-between',
              gap: 8,
              paddingRight: isMobile ? 8 : 16,
              flexWrap: isMobile ? 'wrap' : 'nowrap',
            }}
          >
            <div style={{ overflowX: 'auto', display: 'flex', flex: 1, minWidth: 0, alignItems: 'center' }}>
              <span className="v-meta" style={{ padding: '0 12px', whiteSpace: 'nowrap', fontWeight: 700 }}>
                {inboxUnread > 0 ? `${inboxUnread} unread` : 'Inbox'}
                {totalElements > 0 ? ` · ${totalElements} synced` : ''}
              </span>
              {TABS.map(({ key, label }) => {
                const isActive = activeView === key;
                const count = key !== 'ALL' && key !== 'SENDERS'
                  ? (categoryCounts[key as string] ?? 0)
                  : undefined;

                return (
                  <button
                    key={key}
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

            {activeView !== 'SENDERS' && (
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: isMobile ? 8 : 12,
                  flexShrink: 0,
                  padding: isMobile ? '8px' : 0,
                  width: isMobile ? '100%' : 'auto',
                  justifyContent: isMobile ? 'space-between' : 'flex-start',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '4px 10px', border: '1px solid var(--border)', borderRadius: 16, background: 'var(--bg)', flex: isMobile ? 1 : undefined }}>
                  <Search size={14} style={{ color: 'var(--text-3)' }} />
                  <input
                    type="text"
                    placeholder="Search emails..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    style={{
                      border: 'none',
                      background: 'transparent',
                      fontSize: 12,
                      color: 'var(--text-1)',
                      outline: 'none',
                      width: isMobile ? '100%' : 140,
                      minWidth: 0,
                    }}
                  />
                  {searchQuery && (
                    <button
                      onClick={() => setSearchQuery('')}
                      style={{
                        background: 'none',
                        border: 'none',
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        padding: 0,
                      }}
                    >
                      <X size={12} style={{ color: 'var(--text-3)' }} />
                    </button>
                  )}
                </div>

                {!isMobile && (
                  <span style={{ fontSize: 12, color: 'var(--text-2)' }}>
                    {displayedEmails.length} messages
                  </span>
                )}

                <button
                  onClick={() => setUnreadOnly(u => !u)}
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 6,
                    fontSize: 12,
                    fontWeight: 500,
                    color: unreadOnly ? 'var(--accent)' : 'var(--text-2)',
                    background: unreadOnly ? 'var(--accent-soft)' : 'transparent',
                    border: '1px solid var(--border)',
                    cursor: 'pointer',
                    padding: '4px 10px',
                    borderRadius: 16,
                    flexShrink: 0,
                  }}
                >
                  {unreadOnly ? <EyeOff size={14} /> : <Eye size={14} />}
                  {unreadOnly ? 'Unread' : 'All'}
                </button>
              </div>
            )}
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
                    emails={displayedEmails}
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
                  className="animate-fade-in"
                >
                  <EmailDetail
                    emailId={selectedEmail ? selectedEmail.id : parseInt(urlEmailId!)}
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
