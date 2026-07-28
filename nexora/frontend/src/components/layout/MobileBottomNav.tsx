import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { LayoutDashboard, Inbox, Sparkles, Zap, Settings } from 'lucide-react';
import { useEmails } from '../../hooks/useEmails';
import { PulseRing } from '../common/PulseRing';

export const MobileBottomNav: React.FC = () => {
  const location = useLocation();
  const { emails } = useEmails();
  const unreadEmailCount = emails.filter((e) => !e.isRead).length;

  const navItems = [
    { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/inbox', label: 'Inbox', icon: Inbox, badge: unreadEmailCount },
    { to: '/brain', label: 'Brain', icon: Sparkles, isCenter: true },
    { to: '/priority', label: 'Priority', icon: Zap },
    { to: '/settings', label: 'Settings', icon: Settings },
  ];

  return (
    <nav
      style={{
        position: 'fixed',
        bottom: 0,
        left: 0,
        right: 0,
        height: 68,
        paddingBottom: 'calc(env(safe-area-inset-bottom) + 4px)',
        background: 'var(--surface)',
        borderTop: '1px solid var(--border)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-around',
        zIndex: 50,
        userSelect: 'none',
        animation: 'slideUp 0.3s ease-out',
        backdropFilter: 'blur(10px)',
      }}
    >
      {navItems.map(({ to, label, icon: Icon, badge, isCenter }) => {
        const isActive = location.pathname === to || (to !== '/dashboard' && location.pathname.startsWith(to));

        if (isCenter) {
          return (
            <Link
              key={to}
              to={to}
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                marginTop: -20,
                textDecoration: 'none',
              }}
            >
              <div
                style={{
                  width: 52,
                  height: 52,
                  borderRadius: '50%',
                  background: 'var(--violet)',
                  color: '#ffffff',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  boxShadow: '0 4px 14px rgba(108, 76, 255, 0.4)',
                  transition: 'transform 0.2s ease',
                }}
              >
                <Icon size={24} />
              </div>
              <span
                style={{
                  fontSize: 10,
                  fontWeight: 700,
                  color: isActive ? 'var(--violet)' : 'var(--text-2)',
                  marginTop: 2,
                }}
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
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 3,
              flex: 1,
              textDecoration: 'none',
              color: isActive ? 'var(--primary)' : 'var(--text-2)',
              position: 'relative',
              padding: '8px 0',
              transition: 'all 0.2s ease',
            }}
            onMouseEnter={e => {
              (e.currentTarget as HTMLElement).style.color = 'var(--primary)';
              (e.currentTarget as HTMLElement).style.transform = 'scale(1.1)';
            }}
            onMouseLeave={e => {
              if (!isActive) {
                (e.currentTarget as HTMLElement).style.color = 'var(--text-2)';
              }
              (e.currentTarget as HTMLElement).style.transform = 'scale(1)';
            }}
          >
            <div style={{ position: 'relative' }}>
              <Icon size={20} />
              {badge !== undefined && badge > 0 && (
                <span
                  style={{
                    position: 'absolute',
                    top: -4,
                    right: -8,
                    background: 'var(--ember)',
                    color: '#ffffff',
                    fontSize: 9,
                    fontWeight: 800,
                    borderRadius: 9999,
                    padding: '1px 5px',
                    minWidth: 14,
                    textAlign: 'center',
                  }}
                >
                  {badge > 99 ? '99+' : badge}
                </span>
              )}
            </div>
            <span
              style={{
                fontSize: 10,
                fontWeight: isActive ? 700 : 500,
              }}
            >
              {label}
            </span>
            {isActive && <PulseRing color="ember" size={4} active />}
          </Link>
        );
      })}
    </nav>
  );
};
