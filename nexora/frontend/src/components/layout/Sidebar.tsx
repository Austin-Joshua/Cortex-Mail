import React from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard, Inbox, Sparkles, BarChart2, Bell, Settings, RefreshCw,
  Zap, Clock, Archive, FileText, Share2, HelpCircle,
} from 'lucide-react';
import { useNotificationStore } from '../../store/notificationStore';
import { useEmails } from '../../hooks/useEmails';
import { CAT_COLORS } from '../../utils/catColors';

interface SidebarProps {
  collapsed: boolean;
}

type Badge = 'unread' | 'notif' | 'hot';

interface NavItem {
  to: string;
  icon: React.ComponentType<{ size?: number }>;
  label: string;
  badge?: Badge;
}

const SECTIONS: { title: string; items: NavItem[] }[] = [
  {
    title: 'Workspace',
    items: [
      { to: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
      { to: '/inbox',     icon: Inbox,           label: 'Inbox', badge: 'unread' },
      { to: '/priority',  icon: Zap,             label: 'Priority', badge: 'hot' },
      { to: '/scheduled', icon: Clock,           label: 'Scheduled' },
    ],
  },
  {
    title: 'Compose',
    items: [
      { to: '/brain',   icon: Sparkles, label: 'Velocity Brain' },
      { to: '/drafts',  icon: FileText, label: 'Drafts' },
      { to: '/archive', icon: Archive,  label: 'Archive' },
      { to: '/shared',  icon: Share2,   label: 'Shared' },
    ],
  },
  {
    title: 'Signals',
    items: [
      { to: '/analytics',     icon: BarChart2, label: 'Analytics' },
      { to: '/notifications', icon: Bell,      label: 'Notifications', badge: 'notif' },
    ],
  },
];

const SYSTEM: NavItem[] = [
  { to: '/settings', icon: Settings,   label: 'Settings' },
  { to: '/help',     icon: HelpCircle, label: 'Help' },
];

export const Sidebar: React.FC<SidebarProps> = ({ collapsed }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const { unreadCount: notifCount } = useNotificationStore();
  const { sync, isSyncing, categoryCounts, emails } = useEmails();

  const unreadCount = emails.filter((e) => !e.isRead).length;

  const isOn = (to: string) =>
    location.pathname === to || (to !== '/dashboard' && location.pathname.startsWith(to));

  const badgeFor = (badge?: Badge) => {
    if (badge === 'unread') return unreadCount > 0 ? (unreadCount > 99 ? '99+' : `${unreadCount}`) : null;
    if (badge === 'notif')  return notifCount > 0 ? (notifCount > 99 ? '99+' : `${notifCount}`) : null;
    if (badge === 'hot')    return unreadCount > 0 ? '!' : null;
    return null;
  };

  const renderItem = ({ to, icon: Icon, label, badge }: NavItem) => {
    const on = isOn(to);
    const badgeText = badgeFor(badge);

    if (collapsed) {
      return (
        <Link
          key={to}
          to={to}
          title={label}
          aria-label={label}
          aria-current={on ? 'page' : undefined}
          style={{
            position: 'relative',
            width: 40,
            height: 40,
            margin: '0 auto',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            borderRadius: 'var(--v-r-chip)',
            background: on ? 'var(--v-signal-wash)' : 'transparent',
            color: on ? 'var(--v-signal)' : 'var(--v-ink-2)',
            transition: 'all var(--v-fast)',
          }}
        >
          <Icon size={18} />
          {badgeText && (
            <span
              style={{
                position: 'absolute',
                top: 3,
                right: 3,
                width: 7,
                height: 7,
                borderRadius: 999,
                background: badge === 'hot' ? 'var(--v-ember)' : 'var(--v-signal)',
              }}
            />
          )}
        </Link>
      );
    }

    return (
      <Link
        key={to}
        to={to}
        aria-current={on ? 'page' : undefined}
        className={`rail-item${on ? ' rail-item-on' : ''}`}
      >
        <Icon size={17} />
        <span className="truncate">{label}</span>
        {badgeText && (
          <span className={`rail-badge${badge === 'hot' ? ' rail-badge-hot' : ''}`}>{badgeText}</span>
        )}
      </Link>
    );
  };

  return (
    <aside
      style={{
        width: collapsed ? 68 : 244,
        minWidth: collapsed ? 68 : 244,
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        flexShrink: 0,
        background: 'var(--v-ground)',
        borderRight: '1px solid var(--v-hairline)',
        transition: 'width var(--v-base)',
        userSelect: 'none',
      }}
    >
      {/* Sync — the single primary action */}
      <div style={{ padding: '14px 14px 10px' }}>
        <button
          onClick={() => sync()}
          disabled={isSyncing}
          title="Sync Gmail"
          className="vbtn vbtn-signal"
          style={{ width: '100%', padding: collapsed ? 0 : '0 14px', height: 40 }}
        >
          <RefreshCw size={16} className={isSyncing ? 'animate-spin' : ''} />
          {!collapsed && (isSyncing ? 'Syncing…' : 'Sync inbox')}
        </button>
      </div>

      {/* Navigation */}
      <div
        className="v-scroll"
        style={{ flex: 1, overflowY: 'auto', overflowX: 'hidden', padding: '4px 12px 12px' }}
      >
        {SECTIONS.map((section) => (
          <div key={section.title} style={{ marginBottom: 14 }}>
            {!collapsed && (
              <div className="v-label" style={{ padding: '10px 11px 7px' }}>{section.title}</div>
            )}
            <nav style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              {section.items.map(renderItem)}
            </nav>
          </div>
        ))}

        {!collapsed && (
          <div>
            <div className="v-label" style={{ padding: '10px 11px 7px' }}>Categories</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
              {Object.keys(CAT_COLORS).map((cat) => {
                const cfg = CAT_COLORS[cat];
                const count = (categoryCounts as Record<string, number>)[cat] ?? 0;
                if (count === 0) return null;
                const on = location.pathname === '/inbox' && location.search.includes(`category=${cat}`);

                return (
                  <button
                    key={cat}
                    onClick={() => navigate(`/inbox?category=${cat}`)}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 10,
                      width: '100%',
                      height: 32,
                      padding: '0 11px',
                      borderRadius: 9,
                      border: 'none',
                      cursor: 'pointer',
                      background: on ? 'var(--v-panel-2)' : 'transparent',
                      color: on ? 'var(--v-ink)' : 'var(--v-ink-2)',
                      fontSize: 12.5,
                      fontWeight: on ? 700 : 500,
                      transition: 'all var(--v-fast)',
                    }}
                  >
                    <span className="dot" style={{ ['--dot' as string]: cfg.color } as React.CSSProperties} />
                    <span className="truncate" style={{ flex: 1, textAlign: 'left' }}>{cfg.label}</span>
                    <span className="v-num" style={{ fontSize: 11, color: 'var(--v-ink-3)', fontWeight: 700 }}>
                      {count}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>
        )}
      </div>

      {/* System */}
      <div style={{ borderTop: '1px solid var(--v-hairline)', padding: '10px 12px' }}>
        <nav style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          {SYSTEM.map(renderItem)}
        </nav>
      </div>
    </aside>
  );
};
