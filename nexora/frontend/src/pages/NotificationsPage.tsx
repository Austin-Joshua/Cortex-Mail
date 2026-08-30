import React, { useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { AppShell } from '../components/layout/AppShell';
import { NotificationItem } from '../components/notifications/NotificationItem';
import { notificationApi } from '../api/notificationApi';
import { queryKeys } from '../api/queryKeys';
import { useNotificationStore } from '../store/notificationStore';
import { Bell, CheckCheck } from 'lucide-react';

export const NotificationsPage: React.FC = () => {
  const { notifications, setNotifications, markAllRead } = useNotificationStore();

  const { data, isLoading } = useQuery({
    queryKey: queryKeys.notificationsPage,
    queryFn: notificationApi.getNotifications,
  });

  useEffect(() => {
    if (data) setNotifications(data);
  }, [data, setNotifications]);

  const unread = notifications.filter(n => !n.isRead);
  const read   = notifications.filter(n =>  n.isRead);

  return (
    <AppShell title="Notifications" subtitle="Stay on top of what matters">
      <div style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 14, maxWidth: 800, margin: '0 auto', width: '100%' }}>
        {unread.length > 0 && (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }} className="animate-fade-in">
            <span style={{ padding: '2px 8px', background: 'var(--accent-soft)', borderRadius: 12, fontSize: 11, color: 'var(--accent)', fontWeight: 700 }}>
              {unread.length} new
            </span>
            <button onClick={markAllRead} className="btn-outline" style={{ padding: '4px 10px', fontSize: 11 }}>
              <CheckCheck size={12} /> Mark all read
            </button>
          </div>
        )}

        {isLoading ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {[...Array(4)].map((_, i) => <div key={i} className="skeleton" style={{ height: 52 }} />)}
          </div>
        ) : notifications.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '60px 20px' }} className="animate-fade-in">
            <div style={{ width: 52, height: 52, borderRadius: '50%', background: 'var(--surface)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 16px' }}>
              <Bell size={24} style={{ color: 'var(--text-3)' }} />
            </div>
            <h3 style={{ fontSize: 16, fontWeight: 700, color: 'var(--text-1)', margin: '0 0 6px' }}>All caught up!</h3>
            <p style={{ fontSize: 13, color: 'var(--text-2)', margin: 0 }}>No notifications yet.</p>
          </div>
        ) : (
          <>
            {unread.length > 0 && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                <span className="section-label">NEW ALERTS</span>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                  {unread.map((n) => <NotificationItem key={n.id} notification={n} />)}
                </div>
              </div>
            )}
            {read.length > 0 && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                <span className="section-label">EARLIER</span>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6, opacity: 0.7 }}>
                  {read.map((n) => <NotificationItem key={n.id} notification={n} />)}
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </AppShell>
  );
};
