import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { LayoutDashboard, Inbox, Sparkles, Zap, Settings } from 'lucide-react';
import { useEmails } from '../../hooks/useEmails';
import { useQuery } from '@tanstack/react-query';
import { emailApi } from '../../api/emailApi';

const ITEMS = [
  { to: '/dashboard', label: 'Home',     icon: LayoutDashboard },
  { to: '/inbox',     label: 'Inbox',    icon: Inbox, badged: true },
  { to: '/brain',     label: 'Brain',    icon: Sparkles, center: true },
  { to: '/priority',  label: 'Priority', icon: Zap },
  { to: '/settings',  label: 'Settings', icon: Settings },
];

export const MobileBottomNav: React.FC = () => {
  const location = useLocation();
  const { emails } = useEmails();
  const { data: labelData } = useQuery({
    queryKey: ['gmail-label-counts'],
    queryFn: emailApi.getGmailLabelCounts,
    staleTime: 120_000,
  });
  const unread = labelData?.INBOX?.messagesUnread ?? emails.filter((e) => !e.isRead).length;

  return (
    <nav
      style={{
        position: 'fixed',
        insetInline: 0,
        bottom: 0,
        paddingBottom: 'env(safe-area-inset-bottom)',
        background: 'color-mix(in srgb, var(--v-ground) 88%, transparent)',
        backdropFilter: 'blur(14px)',
        WebkitBackdropFilter: 'blur(14px)',
        borderTop: '1px solid var(--v-hairline)',
        display: 'flex',
        alignItems: 'stretch',
        zIndex: 50,
        userSelect: 'none',
      }}
    >
      {ITEMS.map(({ to, label, icon: Icon, badged, center }) => {
        const on =
          location.pathname === to || (to !== '/dashboard' && location.pathname.startsWith(to));

        if (center) {
          return (
            <Link
              key={to}
              to={to}
              aria-label={label}
              style={{
                flex: 1,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 4,
                padding: '8px 0 10px',
                textDecoration: 'none',
              }}
            >
              <span
                style={{
                  width: 42,
                  height: 34,
                  borderRadius: 11,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  background: 'var(--v-signal)',
                  color: '#fff',
                  boxShadow: on ? 'var(--v-glow)' : 'var(--v-lift-1)',
                  transition: 'box-shadow var(--v-fast)',
                }}
              >
                <Icon size={19} />
              </span>
              <span
                className="v-label"
                style={{ color: on ? 'var(--v-signal)' : 'var(--v-ink-3)', fontSize: 9 }}
              >
                {label}
              </span>
            </Link>
          );
        }

        return (
          <Link
            key={to}
            to={to}
            aria-label={label}
            aria-current={on ? 'page' : undefined}
            style={{
              flex: 1,
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 5,
              padding: '10px 0 12px',
              textDecoration: 'none',
              color: on ? 'var(--v-signal)' : 'var(--v-ink-3)',
              transition: 'color var(--v-fast)',
              position: 'relative',
            }}
          >
            {on && (
              <span
                style={{
                  position: 'absolute',
                  top: 0,
                  width: 22,
                  height: 2.5,
                  borderRadius: '0 0 3px 3px',
                  background: 'var(--v-signal)',
                }}
              />
            )}
            <span style={{ position: 'relative', display: 'flex' }}>
              <Icon size={19} />
              {badged && unread > 0 && (
                <span
                  style={{
                    position: 'absolute',
                    top: -6,
                    left: '58%',
                    minWidth: 16,
                    height: 16,
                    padding: '0 4px',
                    borderRadius: 999,
                    background: 'var(--v-ember)',
                    color: '#FFFFFF',
                    fontSize: 9,
                    fontWeight: 800,
                    fontVariantNumeric: 'tabular-nums',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  {unread > 99 ? '99+' : unread}
                </span>
              )}
            </span>
            <span className="v-label" style={{ color: 'inherit', fontSize: 9 }}>{label}</span>
          </Link>
        );
      })}
    </nav>
  );
};
