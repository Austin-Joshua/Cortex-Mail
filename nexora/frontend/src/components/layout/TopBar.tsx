import React, { useState, useEffect } from 'react';
import { Menu, Search, SlidersHorizontal, Sun, Moon, Bell, RefreshCw, LogOut, Settings as SettingsIcon, CheckCircle } from 'lucide-react';
import { useNotificationStore } from '../../store/notificationStore';
import { NotificationPanel } from '../notifications/NotificationPanel';
import { useEmails } from '../../hooks/useEmails';
import { useEmailStore } from '../../store/emailStore';
import { useAuthStore } from '../../store/authStore';
import { useAuth } from '../../hooks/useAuth';
import { useNavigate } from 'react-router-dom';
import { CAT_COLORS } from '../../utils/catColors';
import { formatRelative } from '../../utils/formatDate';

interface TopBarProps {
  onToggleSidebar: () => void;
}

export const TopBar: React.FC<TopBarProps> = ({ onToggleSidebar }) => {
  const { unreadCount, togglePanel, isPanelOpen } = useNotificationStore();
  const { isSyncing } = useEmails();
  const { setSearchQuery, searchQuery, setActiveCategory } = useEmailStore();
  const { user } = useAuthStore();
  const { handleLogout } = useAuth();
  const navigate = useNavigate();

  const [searchFocused, setSearchFocused] = useState(false);
  const [showFilterDropdown, setShowFilterDropdown] = useState(false);
  const [showAccountMenu, setShowAccountMenu] = useState(false);

  const [theme, setTheme] = useState<'dark' | 'light'>(() => {
    const saved = localStorage.getItem('theme');
    if (saved === 'dark' || saved === 'light') return saved;
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  });

  useEffect(() => {
    if (theme === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
    localStorage.setItem('theme', theme);
  }, [theme]);

  const initials = user?.name
    ? user.name.split(' ').map((n: string) => n[0]).join('').slice(0, 2).toUpperCase()
    : 'NX';

  return (
    <header
      style={{
        height: 64,
        background: 'var(--bg)',
        borderBottom: '1px solid var(--border)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '0 16px',
        flexShrink: 0,
        gap: 16,
        zIndex: 40,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, width: 232, flexShrink: 0 }}>
        <button
          onClick={onToggleSidebar}
          title="Toggle sidebar"
          style={{
            width: 40,
            height: 40,
            borderRadius: '50%',
            background: 'transparent',
            border: 'none',
            color: 'var(--text-2)',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            transition: 'background-color 0.15s ease',
          }}
          onMouseEnter={e => { (e.currentTarget as HTMLElement).style.background = 'var(--surface)'; }}
          onMouseLeave={e => { (e.currentTarget as HTMLElement).style.background = 'transparent'; }}
        >
          <Menu size={20} />
        </button>

        <div
          onClick={() => navigate('/dashboard')}
          style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}
        >
          <div
            style={{
              width: 32,
              height: 32,
              borderRadius: 8,
              background: 'var(--accent)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#ffffff',
              fontWeight: 800,
              fontSize: 16,
            }}
          >
            N
          </div>
          <span style={{ fontSize: 20, fontWeight: 500, color: 'var(--text-1)', letterSpacing: '-0.02em', fontFamily: 'Google Sans, Roboto, sans-serif' }}>
            Nexora
          </span>
        </div>
      </div>

      <div style={{ flex: 1, maxWidth: 720, position: 'relative' }}>
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            height: 46,
            background: searchFocused ? 'var(--bg)' : 'var(--surface)',
            border: `1px solid ${searchFocused ? 'var(--accent)' : 'transparent'}`,
            boxShadow: searchFocused ? '0 1px 3px rgba(0,0,0,0.15)' : 'none',
            borderRadius: 24,
            padding: '0 16px',
            gap: 12,
            transition: 'all 0.15s ease',
          }}
        >
          <Search size={18} style={{ color: 'var(--text-2)', flexShrink: 0 }} />
          <input
            id="topbar-search"
            type="text"
            placeholder="Search mail in Nexora..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onFocus={() => setSearchFocused(true)}
            onBlur={() => setSearchFocused(false)}
            style={{
              flex: 1,
              height: '100%',
              background: 'transparent',
              border: 'none',
              fontSize: 14,
              color: 'var(--text-1)',
              outline: 'none',
              fontFamily: 'Google Sans, Roboto, sans-serif',
            }}
          />
          <button
            onClick={() => setShowFilterDropdown(prev => !prev)}
            title="Filter options"
            style={{
              background: 'transparent',
              border: 'none',
              color: 'var(--text-2)',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: 4,
              borderRadius: '50%',
            }}
          >
            <SlidersHorizontal size={18} />
          </button>
        </div>

        {showFilterDropdown && (
          <div
            style={{
              position: 'absolute',
              top: 52,
              left: 0,
              right: 0,
              background: 'var(--bg)',
              border: '1px solid var(--border)',
              borderRadius: 12,
              boxShadow: '0 4px 16px rgba(0,0,0,0.15)',
              padding: 16,
              zIndex: 50,
            }}
            className="animate-fade-in"
          >
            <p style={{ fontSize: 12, fontWeight: 700, color: 'var(--text-3)', margin: '0 0 10px', textTransform: 'uppercase' }}>
              FILTER BY CATEGORY
            </p>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginBottom: 12 }}>
              <button
                onClick={() => { setActiveCategory('ALL'); setShowFilterDropdown(false); }}
                style={{ padding: '4px 12px', borderRadius: 12, fontSize: 12, border: '1px solid var(--border)', background: 'var(--surface)', cursor: 'pointer', color: 'var(--text-1)' }}
              >
                All Categories
              </button>
              {Object.keys(CAT_COLORS).map(cat => (
                <button
                  key={cat}
                  onClick={() => { setActiveCategory(cat as any); navigate(`/inbox?category=${cat}`); setShowFilterDropdown(false); }}
                  style={{
                    padding: '4px 10px',
                    borderRadius: 12,
                    fontSize: 12,
                    border: '1px solid var(--border)',
                    background: 'var(--surface)',
                    cursor: 'pointer',
                    color: CAT_COLORS[cat].color,
                    fontWeight: 500,
                  }}
                >
                  {cat}
                </button>
              ))}
            </div>
          </div>
        )}
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexShrink: 0 }}>
        <div style={{ fontSize: 11, color: 'var(--text-3)', display: 'flex', alignItems: 'center', gap: 4 }} title={user?.lastSyncedAt ? `Last synced: ${new Date(user.lastSyncedAt).toLocaleString()}` : 'Never synced'}>
          {isSyncing ? (
            <span style={{ color: 'var(--accent)', display: 'inline-flex', alignItems: 'center', gap: 4 }}>
              <RefreshCw size={12} className="animate-spin" /> Syncing...
            </span>
          ) : (
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
              <CheckCircle size={11} style={{ color: 'var(--success)' }} />
              {user?.lastSyncedAt ? `Synced ${formatRelative(user.lastSyncedAt)}` : 'Synced'}
            </span>
          )}
        </div>

        <button
          onClick={() => setTheme(prev => prev === 'dark' ? 'light' : 'dark')}
          title="Toggle theme"
          style={{
            width: 38,
            height: 38,
            borderRadius: '50%',
            background: 'transparent',
            border: 'none',
            color: 'var(--text-2)',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          {theme === 'dark' ? <Moon size={18} /> : <Sun size={18} />}
        </button>

        <div style={{ position: 'relative' }}>
          <button
            onClick={togglePanel}
            title="Notifications"
            style={{
              width: 38,
              height: 38,
              borderRadius: '50%',
              background: isPanelOpen ? 'var(--surface-2)' : 'transparent',
              border: 'none',
              color: 'var(--text-2)',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              position: 'relative',
            }}
          >
            <Bell size={18} />
            {unreadCount > 0 && (
              <span
                style={{
                  position: 'absolute',
                  top: 8,
                  right: 8,
                  width: 8,
                  height: 8,
                  borderRadius: '50%',
                  background: 'var(--danger)',
                }}
              />
            )}
          </button>
          <NotificationPanel />
        </div>

        <div style={{ position: 'relative' }}>
          <button
            onClick={() => setShowAccountMenu(prev => !prev)}
            style={{
              width: 36,
              height: 36,
              borderRadius: '50%',
              background: 'var(--accent)',
              border: 'none',
              color: '#ffffff',
              fontSize: 13,
              fontWeight: 700,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            {initials}
          </button>

          {showAccountMenu && (
            <div
              style={{
                position: 'absolute',
                top: 44,
                right: 0,
                width: 220,
                background: 'var(--bg)',
                border: '1px solid var(--border)',
                borderRadius: 12,
                boxShadow: '0 4px 16px rgba(0,0,0,0.15)',
                padding: '12px 0',
                zIndex: 50,
              }}
              className="animate-fade-in"
            >
              <div style={{ padding: '0 16px 10px', borderBottom: '1px solid var(--border)' }}>
                <p style={{ fontSize: 13, fontWeight: 700, color: 'var(--text-1)', margin: 0 }}>
                  {user?.name || 'User'}
                </p>
                <p style={{ fontSize: 11, color: 'var(--text-2)', margin: '2px 0 0' }}>
                  {user?.email || ''}
                </p>
              </div>

              <button
                onClick={() => { navigate('/settings'); setShowAccountMenu(false); }}
                style={{
                  width: '100%',
                  padding: '8px 16px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 10,
                  fontSize: 13,
                  color: 'var(--text-1)',
                  background: 'transparent',
                  border: 'none',
                  cursor: 'pointer',
                  textAlign: 'left',
                }}
              >
                <SettingsIcon size={16} /> Settings
              </button>

              <button
                onClick={() => { handleLogout(); setShowAccountMenu(false); }}
                style={{
                  width: '100%',
                  padding: '8px 16px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 10,
                  fontSize: 13,
                  color: 'var(--danger)',
                  background: 'transparent',
                  border: 'none',
                  cursor: 'pointer',
                  textAlign: 'left',
                }}
              >
                <LogOut size={16} /> Logout
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};
