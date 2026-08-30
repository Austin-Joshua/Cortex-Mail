import React from 'react';
import { useNavigate } from 'react-router-dom';
import type { Notification } from '../../types/Notification';
import { notificationApi } from '../../api/notificationApi';
import { useNotificationStore } from '../../store/notificationStore';
import { formatRelative } from '../../utils/formatDate';
import { Bell, Clock, Mail, CheckCircle, Check, ArrowRight } from 'lucide-react';

const TYPE_CONFIG: Record<string, { icon: React.ReactNode; color: string; bg: string; border: string }> = {
  DEADLINE: {
    icon: <Clock size={12} />,
    color: 'var(--color-danger)',
    bg: 'var(--color-danger-soft)',
    border: 'var(--color-border)',
  },
  ACTION_REQUIRED: {
    icon: <CheckCircle size={12} />,
    color: 'var(--color-warning)',
    bg: 'var(--color-warning-soft)',
    border: 'var(--color-border)',
  },
  IMPORTANT_EMAIL: {
    icon: <Mail size={12} />,
    color: 'var(--color-cortex-light)',
    bg: 'var(--color-cortex-soft)',
    border: 'var(--color-cortex-border)',
  },
  DAILY_DIGEST: {
    icon: <Bell size={12} />,
    color: 'var(--text-2)',
    bg: 'var(--surface)',
    border: 'var(--border)',
  },
};

const DEFAULT_CONFIG = TYPE_CONFIG.DAILY_DIGEST;

interface Props {
  notification: Notification;
}

export const NotificationItem: React.FC<Props> = ({ notification }) => {
  const { markRead } = useNotificationStore();
  const navigate = useNavigate();
  const cfg = TYPE_CONFIG[notification.notificationType] ?? DEFAULT_CONFIG;

  const handleMarkRead = async (e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      await notificationApi.markRead(notification.id);
      markRead(notification.id);
    } catch {}
  };

  const handleCardClick = async () => {
    if (!notification.isRead) {
      try {
        await notificationApi.markRead(notification.id);
        markRead(notification.id);
      } catch {}
    }
    if (notification.relatedEmailId) {
      navigate(`/inbox?emailId=${notification.relatedEmailId}`);
    }
  };

  return (
    <div
      onClick={handleCardClick}
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 12,
        padding: '10px 14px',
        background: notification.isRead ? 'transparent' : 'var(--surface)',
        border: '1px solid var(--border)',
        borderRadius: 8,
        cursor: notification.relatedEmailId ? 'pointer' : 'default',
        transition: 'all 0.15s ease',
        position: 'relative',
      }}
      onMouseEnter={e => {
        (e.currentTarget as HTMLElement).style.background = 'var(--surface-2)';
      }}
      onMouseLeave={e => {
        (e.currentTarget as HTMLElement).style.background = notification.isRead ? 'transparent' : 'var(--surface)';
      }}
    >
      {!notification.isRead && (
        <div
          style={{
            position: 'absolute',
            left: 0,
            top: 0,
            bottom: 0,
            width: 3,
            background: 'var(--accent)',
            borderRadius: '3px 0 0 3px',
          }}
        />
      )}

      <div
        style={{
          width: 28,
          height: 28,
          borderRadius: 6,
          background: cfg.bg,
          border: `1px solid ${cfg.border}`,
          color: cfg.color,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          flexShrink: 0,
        }}
      >
        {cfg.icon}
      </div>

      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8, marginBottom: 2 }}>
          <p
            style={{
              fontSize: 13,
              fontWeight: notification.isRead ? 400 : 700,
              color: notification.isRead ? 'var(--text-2)' : 'var(--text-1)',
              margin: 0,
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
            }}
          >
            {notification.title}
          </p>
          <span style={{ fontSize: 11, color: 'var(--text-3)', flexShrink: 0 }}>
            {formatRelative(notification.createdAt)}
          </span>
        </div>
        <p
          style={{
            fontSize: 12,
            color: 'var(--text-2)',
            margin: 0,
            lineHeight: 1.4,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
          }}
        >
          {notification.message}
        </p>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexShrink: 0 }}>
        {!notification.isRead && (
          <button
            onClick={handleMarkRead}
            style={{
              width: 20,
              height: 20,
              borderRadius: 4,
              border: '1px solid var(--border)',
              background: 'transparent',
              color: 'var(--text-2)',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
            title="Mark read"
          >
            <Check size={12} />
          </button>
        )}
        {notification.relatedEmailId && (
          <ArrowRight size={12} style={{ color: 'var(--text-3)' }} />
        )}
      </div>
    </div>
  );
};
