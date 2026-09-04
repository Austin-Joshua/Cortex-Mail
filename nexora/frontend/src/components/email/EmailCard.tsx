import React from 'react';
import type { Email } from '../../types/Email';
import { CategoryTag } from '../common/CategoryTag';
import { PriorityBars } from '../common/PriorityBars';
import { Star, Mail, MailOpen, Brain } from 'lucide-react';
import { emailApi } from '../../api/emailApi';
import { queryKeys } from '../../api/queryKeys';
import { useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';

interface EmailCardProps {
  email: Email;
  onClick?: () => void;
  isSelected?: boolean;
  checked?: boolean;
  selectable?: boolean;
  onToggleSelect?: (id: number) => void;
}

function formatGmailDate(dateStr: string): string {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  if (isNaN(date.getTime())) return '';

  const now = new Date();
  const isToday = date.toDateString() === now.toDateString();
  const isThisYear = date.getFullYear() === now.getFullYear();

  if (isToday) {
    return date.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
  } else if (isThisYear) {
    return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
  } else {
    return date.toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' });
  }
}

export const EmailCard: React.FC<EmailCardProps> = React.memo(({
  email,
  onClick,
  isSelected,
  checked = false,
  selectable = false,
  onToggleSelect,
}) => {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const isUnread = !email.isRead;
  const starred = Boolean(email.isStarred);

  const invalidateMailbox = () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.emails });
    queryClient.invalidateQueries({ queryKey: queryKeys.dashboardSummary });
    queryClient.invalidateQueries({ queryKey: queryKeys.gmailLabelCounts });
    queryClient.invalidateQueries({ queryKey: queryKeys.syncStatus });
  };

  const handleClick = () => {
    onClick?.();
  };

  const handleToggleRead = async (e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      if (email.isRead) {
        await emailApi.markUnread(email.id);
      } else {
        await emailApi.markRead(email.id);
      }
      invalidateMailbox();
    } catch { /* keep current row */ }
  };

  const handleToggleStar = async (e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      await emailApi.setStarred(email.id, !starred);
      invalidateMailbox();
    } catch { /* keep current row */ }
  };

  const handleAskBrain = (e: React.MouseEvent) => {
    e.stopPropagation();
    navigate(`/brain?context=email:${email.id}`);
  };

  const snippetText = email.aiSummary || email.bodySnippet || email.subject || '';

  return (
    <div
      onClick={handleClick}
      className={`gmail-row${isUnread ? ' unread' : ''}${isSelected ? ' selected' : ''}`}
      style={{
        background: isSelected
          ? 'var(--color-surface-active)'
          : 'var(--color-surface)',
      }}
    >
      {selectable && (
        <div
          onClick={(e) => {
            e.stopPropagation();
            onToggleSelect?.(email.id);
          }}
          style={{ padding: '0 8px 0 0', display: 'flex', alignItems: 'center' }}
        >
          <input
            type="checkbox"
            checked={checked}
            onChange={() => onToggleSelect?.(email.id)}
            onClick={(e) => e.stopPropagation()}
            aria-label={checked ? 'Deselect message' : 'Select message'}
            style={{ width: 16, height: 16, cursor: 'pointer', accentColor: 'var(--accent)' }}
          />
        </div>
      )}

      <button
        type="button"
        onClick={handleToggleStar}
        title={starred ? 'Unstar' : 'Star'}
        style={{
          padding: '0 12px 0 0',
          display: 'flex',
          alignItems: 'center',
          background: 'transparent',
          border: 'none',
          cursor: 'pointer',
        }}
      >
        <Star
          size={18}
          style={{
            color: starred ? 'var(--color-star)' : 'var(--color-star-muted)',
            fill: starred ? 'var(--color-star)' : 'transparent',
          }}
        />
      </button>

      <div
        style={{
          width: 180,
          minWidth: 140,
          fontWeight: isUnread ? 700 : 400,
          color: isUnread ? 'var(--text-1)' : 'var(--text-2)',
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap',
          fontSize: 14,
          paddingRight: 12,
          flexShrink: 0,
        }}
      >
        {email.senderName || email.senderEmail}
      </div>

      <div
        style={{
          flex: 1,
          display: 'flex',
          alignItems: 'center',
          gap: 6,
          minWidth: 0,
          fontSize: 14,
          paddingRight: 12,
        }}
      >
        <span
          style={{
            fontWeight: isUnread ? 700 : 400,
            color: isUnread ? 'var(--color-text-primary)' : 'var(--color-text-secondary)',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
            flexShrink: 0,
            maxWidth: '40%',
          }}
        >
          {email.subject || '(no subject)'}
        </span>
        <span style={{ color: 'var(--text-3)' }}>-</span>
        <span
          style={{
            color: 'var(--text-2)',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
            flex: 1,
            fontWeight: 400,
          }}
        >
          {snippetText}
        </span>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0, paddingRight: 16 }}>
        <PriorityBars priority={email.priority as 'HIGH' | 'MEDIUM' | 'LOW'} />
        <CategoryTag category={email.category} />
      </div>

      <div
        className="date-cell"
        style={{
          fontSize: 12,
          fontWeight: isUnread ? 700 : 400,
          color: isUnread ? 'var(--text-1)' : 'var(--text-2)',
          minWidth: 64,
          textAlign: 'right',
          flexShrink: 0,
        }}
      >
        {formatGmailDate(email.receivedAt || '')}
      </div>

      <div
        className="actions-cell"
        style={{
          alignItems: 'center',
          gap: 6,
          background: 'inherit',
          paddingLeft: 8,
          flexShrink: 0,
        }}
      >
        <button
          type="button"
          onClick={handleToggleRead}
          title={email.isRead ? 'Mark as unread' : 'Mark as read'}
          style={{
            background: 'transparent',
            border: 'none',
            color: 'var(--text-2)',
            cursor: 'pointer',
            padding: 4,
            borderRadius: '50%',
          }}
        >
          {email.isRead ? <Mail size={16} /> : <MailOpen size={16} />}
        </button>

        <button
          type="button"
          onClick={handleAskBrain}
          title="Ask Cortex Brain about this email"
          style={{
            background: 'transparent',
            border: 'none',
            color: 'var(--accent)',
            cursor: 'pointer',
            padding: 4,
            borderRadius: '50%',
          }}
        >
          <Brain size={16} />
        </button>
      </div>
    </div>
  );
});
