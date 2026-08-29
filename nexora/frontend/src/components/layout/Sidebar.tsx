import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import {
  LayoutDashboard, Inbox, Sparkles, BarChart2, Settings,
  Zap, Clock, Archive, FileText, Share2, HelpCircle,
} from 'lucide-react';
import { useEmails } from '../../hooks/useEmails';

type Badge = 'unread' | 'hot';

interface NavItem {
  to: string;
  icon: React.ComponentType<{ size?: number }>;
  label: string;
  badge?: Badge;
}

const NAV_ITEMS: NavItem[] = [
  { to: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/inbox',     icon: Inbox,           label: 'Inbox', badge: 'unread' },
  { to: '/priority',  icon: Zap,             label: 'Priority', badge: 'hot' },
  { to: '/scheduled', icon: Clock,           label: 'Scheduled' },
  { to: '/brain',     icon: Sparkles,        label: 'Cortex Brain' },
  { to: '/drafts',    icon: FileText,        label: 'Drafts' },
  { to: '/archive',   icon: Archive,         label: 'Archive' },
  { to: '/shared',    icon: Share2,          label: 'Shared' },
  { to: '/analytics', icon: BarChart2,       label: 'Analytics' },
];

const SYSTEM: NavItem[] = [
  { to: '/settings', icon: Settings,   label: 'Settings' },
  { to: '/help',     icon: HelpCircle, label: 'Help' },
];

export const Sidebar: React.FC = () => {
  const location = useLocation();
  const { inboxUnread } = useEmails();

  const isOn = (to: string) =>
    location.pathname === to || (to !== '/dashboard' && location.pathname.startsWith(to));

  const badgeFor = (badge?: Badge) => {
    if (badge === 'unread') return inboxUnread > 0 ? (inboxUnread > 99 ? '99+' : `${inboxUnread}`) : null;
    if (badge === 'hot')    return inboxUnread > 0 ? '!' : null;
    return null;
  };

  const renderItem = ({ to, icon: Icon, label, badge }: NavItem) => {
    const on = isOn(to);
    const badgeText = badgeFor(badge);

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
      className="app-sidebar"
      style={{
        width: 252,
        minWidth: 252,
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        flexShrink: 0,
        userSelect: 'none',
      }}
    >
      <div
        className="v-scroll"
        style={{ flex: 1, overflowY: 'auto', overflowX: 'hidden', padding: '16px 12px 12px' }}
      >
        <nav style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          {NAV_ITEMS.map(renderItem)}
        </nav>
      </div>

      <div style={{ borderTop: '1px solid var(--v-hairline)', padding: '10px 12px' }}>
        <nav style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          {SYSTEM.map(renderItem)}
        </nav>
      </div>
    </aside>
  );
};
