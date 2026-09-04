import React from 'react';
import { EmailCard } from './EmailCard';
import type { Email } from '../../types/Email';
import { useEmailStore } from '../../store/emailStore';
import { Archive, CheckCheck, Inbox, Mail, MailOpen, Star, Trash2 } from 'lucide-react';

interface EmailListProps {
  emails: Email[];
  isLoading: boolean;
  onEmailSelect?: (email: Email) => void;
  hasMore?: boolean;
  isLoadingMore?: boolean;
  onLoadMore?: () => void;
  selectable?: boolean;
  selectedIds?: Set<number>;
  onToggleSelect?: (id: number) => void;
  onSelectAll?: () => void;
  onClearSelection?: () => void;
  onBulkAction?: (action: 'READ' | 'UNREAD' | 'STAR' | 'UNSTAR' | 'ARCHIVE' | 'TRASH') => void;
  onMarkAllRead?: () => void;
  unreadCount?: number;
  busy?: boolean;
}

export const EmailList: React.FC<EmailListProps> = ({
  emails,
  isLoading,
  onEmailSelect,
  hasMore = false,
  isLoadingMore = false,
  onLoadMore,
  selectable = false,
  selectedIds,
  onToggleSelect,
  onSelectAll,
  onClearSelection,
  onBulkAction,
  onMarkAllRead,
  unreadCount = 0,
  busy = false,
}) => {
  const { selectedEmail } = useEmailStore();
  const selectedCount = selectedIds?.size ?? 0;
  const allSelected = emails.length > 0 && selectedCount === emails.length;

  if (isLoading) {
    return (
      <div style={{ background: 'var(--bg)' }}>
        {Array.from({ length: 10 }).map((_, i) => (
          <div
            key={i}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 16,
              height: 44,
              padding: '0 16px',
              borderBottom: '1px solid var(--border)',
            }}
          >
            <div className="skeleton" style={{ width: 16, height: 16, borderRadius: 2 }} />
            <div className="skeleton" style={{ width: 18, height: 18, borderRadius: 2 }} />
            <div className="skeleton" style={{ width: 140, height: 14, borderRadius: 4 }} />
            <div className="skeleton" style={{ flex: 1, height: 14, borderRadius: 4 }} />
            <div className="skeleton" style={{ width: 64, height: 14, borderRadius: 4 }} />
          </div>
        ))}
      </div>
    );
  }

  if (emails.length === 0) {
    return (
      <div
        className="animate-fade-in"
        style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '64px 24px',
          textAlign: 'center',
          background: 'var(--bg)',
        }}
      >
        <div
          style={{
            width: 56,
            height: 56,
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            marginBottom: 16,
            background: 'var(--surface)',
            border: '1px solid var(--border)',
          }}
        >
          <Inbox size={24} style={{ color: 'var(--text-3)' }} />
        </div>
        <p style={{ color: 'var(--text-1)', fontWeight: 700, fontSize: 14, margin: '0 0 4px', fontFamily: 'Google Sans, Roboto, sans-serif' }}>
          Nothing here
        </p>
        <p style={{ color: 'var(--text-2)', fontSize: 13, margin: 0 }}>
          Sync from the title bar if this view should have mail.
        </p>
      </div>
    );
  }

  return (
    <div style={{ background: 'var(--bg)' }}>
      {selectable && (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            padding: '8px 12px',
            borderBottom: '1px solid var(--border)',
            flexWrap: 'wrap',
          }}
        >
          <input
            type="checkbox"
            checked={allSelected}
            onChange={() => (allSelected ? onClearSelection?.() : onSelectAll?.())}
            aria-label={allSelected ? 'Clear selection' : 'Select all visible'}
            style={{ width: 16, height: 16, accentColor: 'var(--accent)' }}
          />
          {selectedCount > 0 ? (
            <>
              <span className="v-meta">{selectedCount} selected</span>
              <button type="button" className="vbtn vbtn-bare" disabled={busy} onClick={() => onBulkAction?.('READ')}>
                <MailOpen size={14} /> Read
              </button>
              <button type="button" className="vbtn vbtn-bare" disabled={busy} onClick={() => onBulkAction?.('UNREAD')}>
                <Mail size={14} /> Unread
              </button>
              <button type="button" className="vbtn vbtn-bare" disabled={busy} onClick={() => onBulkAction?.('STAR')}>
                <Star size={14} /> Star
              </button>
              <button type="button" className="vbtn vbtn-bare" disabled={busy} onClick={() => onBulkAction?.('ARCHIVE')}>
                <Archive size={14} /> Archive
              </button>
              <button type="button" className="vbtn vbtn-bare" disabled={busy} onClick={() => onBulkAction?.('TRASH')}>
                <Trash2 size={14} /> Trash
              </button>
              <button type="button" className="vbtn vbtn-bare" disabled={busy} onClick={() => onClearSelection?.()}>
                Clear
              </button>
            </>
          ) : (
            <>
              <span className="v-meta">{emails.length} shown</span>
              {unreadCount > 0 && (
                <button type="button" className="vbtn vbtn-bare" disabled={busy} onClick={() => onMarkAllRead?.()}>
                  <CheckCheck size={14} /> Mark all as read
                </button>
              )}
            </>
          )}
        </div>
      )}
      {emails.map((email) => (
        <EmailCard
          key={email.id}
          email={email}
          isSelected={selectedEmail?.id === email.id}
          selectable={selectable}
          checked={selectedIds?.has(email.id) ?? false}
          onToggleSelect={onToggleSelect}
          onClick={() => onEmailSelect?.(email)}
        />
      ))}
      {hasMore && (
        <div style={{ padding: '12px 16px', borderTop: '1px solid var(--border)' }}>
          <button
            type="button"
            className="vbtn vbtn-quiet"
            style={{ width: '100%', height: 36 }}
            disabled={isLoadingMore}
            onClick={() => onLoadMore?.()}
          >
            {isLoadingMore ? 'Loading…' : 'Load more'}
          </button>
        </div>
      )}
    </div>
  );
};
