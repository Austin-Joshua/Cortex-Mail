import React from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard, Inbox, Sparkles, BarChart2, Bell, Settings, RefreshCw,
  Zap, Clock, Archive, FileText, Share2, HelpCircle
} from 'lucide-react';
import { useNotificationStore } from '../../store/notificationStore';
import { useEmails } from '../../hooks/useEmails';
import { CAT_COLORS } from '../../utils/catColors';

interface SidebarProps {
  collapsed: boolean;
}

const MAIN_NAV = [
  { to: '/dashboard',     icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/inbox',         icon: Inbox,           label: 'Inbox', badge: 'unread' },
  { to: '/priority',      icon: Zap,             label: 'Priority', badge: 'hot' },
  { to: '/scheduled',     icon: Clock,           label: 'Scheduled' },
];

const FEATURES_NAV = [
  { to: '/brain',         icon: Sparkles,        label: 'AI Brain' },
  { to: '/drafts',        icon: FileText,        label: 'Drafts', badge: 'count' },
  { to: '/archive',       icon: Archive,         label: 'Archive' },
  { to: '/shared',        icon: Share2,          label: 'Shared' },
];

const INSIGHTS_NAV = [
  { to: '/analytics',     icon: BarChart2,       label: 'Analytics' },
  { to: '/notifications', icon: Bell,            label: 'Notifications', badge: 'notif' },
];

const SYSTEM_NAV = [
  { to: '/settings',      icon: Settings,        label: 'Settings' },
  { to: '/help',          icon: HelpCircle,      label: 'Help & Support' },
];

const NavSection: React.FC<{
  title?: string;
  items: typeof MAIN_NAV;
  collapsed: boolean;
  location: ReturnType<typeof useLocation>;
  unreadEmailCount: number;
  unreadNotifCount: number;
  emails: any[];
}> = ({ title, items, collapsed, location, unreadEmailCount, unreadNotifCount }) => {
  return (
    <>
      {title && !collapsed && (
        <div style={{
          padding: '16px 16px 8px 16px',
          fontSize: '11px',
          fontWeight: 700,
          letterSpacing: '0.08em',
          textTransform: 'uppercase',
          color: 'var(--text-3)',
        }}>
          {title}
        </div>
      )}
      <nav style={{ display: 'flex', flexDirection: 'column', gap: 2, padding: collapsed ? '4px 8px' : '0 8px' }}>
        {items.map(({ to, icon: Icon, label, badge }) => {
          const isActive = location.pathname === to || (to !== '/dashboard' && location.pathname.startsWith(to));
          let badgeContent = null;

          if (badge === 'unread') badgeContent = unreadEmailCount > 0 ? unreadEmailCount : null;
          if (badge === 'notif') badgeContent = unreadNotifCount > 0 ? unreadNotifCount : null;
          if (badge === 'hot') badgeContent = '!';

          if (collapsed) {
            return (
              <Link
                key={to}
                to={to}
                title={label}
                style={{
                  width: 40,
                  height: 40,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  borderRadius: 10,
                  background: isActive ? 'var(--primary)' : 'transparent',
                  color: isActive ? 'white' : 'var(--text-2)',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease',
                  position: 'relative',
                }}
              >
                <Icon size={18} />
                {badgeContent && (
                  <div style={{
                    position: 'absolute',
                    top: -4,
                    right: -4,
                    background: 'var(--accent)',
                    color: 'white',
                    borderRadius: '50%',
                    width: 18,
                    height: 18,
                    fontSize: 10,
                    fontWeight: 700,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}>
                    {badgeContent}
                  </div>
                )}
              </Link>
            );
          }

          return (
            <Link
              key={to}
              to={to}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                padding: '10px 12px',
                borderRadius: 10,
                background: isActive ? 'var(--primary-pale)' : 'transparent',
                color: isActive ? 'var(--primary)' : 'var(--text-2)',
                textDecoration: 'none',
                cursor: 'pointer',
                transition: 'all 0.2s ease',
                fontWeight: isActive ? 600 : 500,
                fontSize: 14,
                position: 'relative',
              }}
            >
              <Icon size={18} />
              <span style={{ flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {label}
              </span>
              {badgeContent && (
                <span style={{
                  background: 'var(--accent)',
                  color: 'white',
                  borderRadius: 12,
                  padding: '2px 8px',
                  fontSize: 12,
                  fontWeight: 700,
                }}>
                  {badgeContent}
                </span>
              )}
            </Link>
          );
        })}
      </nav>
    </>
  );
};

export const Sidebar: React.FC<SidebarProps> = ({ collapsed }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const { unreadCount: unreadNotifCount } = useNotificationStore();
  const { sync, isSyncing, categoryCounts, emails } = useEmails();

  const unreadEmailCount = emails.filter(e => !e.isRead).length;

  return (
    <aside style={{
      width: collapsed ? 72 : 260,
      minWidth: collapsed ? 72 : 260,
      height: '100%',
      display: 'flex',
      flexDirection: 'column',
      flexShrink: 0,
      background: 'var(--bg)',
      borderRight: '1px solid var(--border)',
      transition: 'width 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
      overflow: 'hidden',
      userSelect: 'none',
    }}>
      {/* Header with Sync Button */}
      <div style={{
        padding: collapsed ? '12px 8px' : '16px',
        borderBottom: '1px solid var(--border)',
      }}>
        <button
          onClick={() => sync()}
          disabled={isSyncing}
          style={{
            width: collapsed ? 44 : '100%',
            height: 44,
            borderRadius: collapsed ? 10 : 12,
            background: 'var(--primary)',
            color: 'white',
            border: 'none',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: collapsed ? 'center' : 'flex-start',
            gap: 10,
            padding: collapsed ? 0 : '0 14px',
            fontWeight: 600,
            fontSize: 14,
            transition: 'all 0.2s ease',
          }}
          title="Sync Gmail inbox"
        >
          <RefreshCw size={18} className={isSyncing ? 'animate-spin' : ''} />
          {!collapsed && (isSyncing ? 'Syncing...' : 'Sync Inbox')}
        </button>
      </div>

      {/* Navigation Sections */}
      <div style={{ flex: 1, overflowY: 'auto', overflowX: 'hidden' }}>
        <NavSection
          title="MAIN"
          items={MAIN_NAV}
          collapsed={collapsed}
          location={location}
          unreadEmailCount={unreadEmailCount}
          unreadNotifCount={unreadNotifCount}
          emails={emails}
        />

        <NavSection
          title="FEATURES"
          items={FEATURES_NAV}
          collapsed={collapsed}
          location={location}
          unreadEmailCount={unreadEmailCount}
          unreadNotifCount={unreadNotifCount}
          emails={emails}
        />

        <NavSection
          title="INSIGHTS"
          items={INSIGHTS_NAV}
          collapsed={collapsed}
          location={location}
          unreadEmailCount={unreadEmailCount}
          unreadNotifCount={unreadNotifCount}
          emails={emails}
        />

        {/* Categories Section */}
        {!collapsed && (
          <div style={{ padding: '16px 8px' }}>
            <div style={{
              padding: '8px 16px',
              fontSize: '11px',
              fontWeight: 700,
              letterSpacing: '0.08em',
              textTransform: 'uppercase',
              color: 'var(--text-3)',
            }}>
              CATEGORIES
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
              {Object.keys(CAT_COLORS).map(cat => {
                const cfg = CAT_COLORS[cat];
                const count = categoryCounts[cat] ?? 0;
                const isCatActive = location.pathname === '/inbox' && location.search.includes(`category=${cat}`);

                return (
                  <div
                    key={cat}
                    onClick={() => navigate(`/inbox?category=${cat}`)}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 10,
                      padding: '8px 12px',
                      borderRadius: 8,
                      cursor: 'pointer',
                      background: isCatActive ? 'var(--surface-2)' : 'transparent',
                      color: isCatActive ? 'var(--text-1)' : 'var(--text-2)',
                      fontSize: 13,
                      fontWeight: isCatActive ? 600 : 400,
                      transition: 'all 0.2s ease',
                    }}
                  >
                    <span style={{
                      width: 8,
                      height: 8,
                      borderRadius: 2,
                      background: cfg.color,
                      flexShrink: 0,
                    }} />
                    <span style={{ flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {cfg.label}
                    </span>
                    {count > 0 && (
                      <span style={{ fontSize: 11, color: 'var(--text-3)', fontWeight: 600 }}>
                        {count}
                      </span>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>

      {/* System Navigation */}
      <NavSection
        items={SYSTEM_NAV}
        collapsed={collapsed}
        location={location}
        unreadEmailCount={unreadEmailCount}
        unreadNotifCount={unreadNotifCount}
        emails={emails}
      />
    </aside>
  );
};
