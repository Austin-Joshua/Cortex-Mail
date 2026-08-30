import React, { useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { notificationApi } from '../../api/notificationApi';
import { queryKeys } from '../../api/queryKeys';
import { useNotificationStore } from '../../store/notificationStore';
import { NotificationItem } from './NotificationItem';
import { Bell, X, CheckCheck, ExternalLink } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export const NotificationPanel: React.FC = () => {
  const { notifications, setNotifications, isPanelOpen, togglePanel, markAllRead } = useNotificationStore();
  const navigate = useNavigate();

  const { data } = useQuery({
    queryKey: queryKeys.notifications,
    queryFn: notificationApi.getNotifications,
    enabled: isPanelOpen,
  });

  useEffect(() => {
    if (data) setNotifications(data);
  }, [data, setNotifications]);

  if (!isPanelOpen) return null;

  const unreadCount = notifications.filter(n => !n.isRead).length;

  return (
    <>
      <div style={{ position: 'fixed', inset: 0, zIndex: 40 }} onClick={togglePanel} />

      <div
        className="animate-fade-in"
        style={{
          position: 'absolute',
          right: 0,
          top: 44,
          width: 320,
          maxHeight: 400,
          zIndex: 50,
          display: 'flex',
          flexDirection: 'column',
          background: 'var(--bg)',
          border: '1px solid var(--border)',
          borderRadius: 12,
          boxShadow: '0 4px 16px rgba(0,0,0,0.15)',
        }}
      >
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '12px 16px',
            borderBottom: '1px solid var(--border)',
            flexShrink: 0,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Bell size={16} style={{ color: 'var(--accent)' }} />
            <span style={{ fontSize: 13, fontWeight: 700, color: 'var(--text-1)' }}>Notifications</span>
            {unreadCount > 0 && (
              <span style={{ padding: '2px 6px', background: 'var(--accent-soft)', borderRadius: 12, fontSize: 10, fontWeight: 700, color: 'var(--accent)' }}>
                {unreadCount}
              </span>
            )}
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            {unreadCount > 0 && (
              <button
                onClick={markAllRead}
                style={{ background: 'none', border: 'none', color: 'var(--accent)', fontSize: 12, fontWeight: 600, cursor: 'pointer' }}
              >
                <CheckCheck size={14} />
              </button>
            )}
            <button onClick={togglePanel} style={{ background: 'none', border: 'none', color: 'var(--text-3)', cursor: 'pointer' }}>
              <X size={16} />
            </button>
          </div>
        </div>

        <div style={{ flex: 1, overflowY: 'auto', padding: 8, display: 'flex', flexDirection: 'column', gap: 6 }}>
          {notifications.length === 0 ? (
            <div style={{ padding: '24px 0', textAlign: 'center', color: 'var(--text-2)', fontSize: 13 }}>
              No notifications
            </div>
          ) : (
            notifications.slice(0, 5).map((n) => (
              <NotificationItem key={n.id} notification={n} />
            ))
          )}
        </div>

        {notifications.length > 0 && (
          <div style={{ padding: '8px 12px', borderTop: '1px solid var(--border)', flexShrink: 0 }}>
            <button
              onClick={() => { togglePanel(); navigate('/notifications'); }}
              className="btn-outline"
              style={{ width: '100%', justifyContent: 'center', fontSize: 12 }}
            >
              View all notifications <ExternalLink size={12} />
            </button>
          </div>
        )}
      </div>
    </>
  );
};
