import React, { useState, useEffect, useRef } from 'react';
import {
  PanelLeft, Search, Sun, Moon, Bell, RefreshCw, LogOut, X,
  Settings as SettingsIcon, ChevronDown,
} from 'lucide-react';
import { useNotificationStore } from '../../store/notificationStore';
import { NotificationPanel } from '../notifications/NotificationPanel';
import { useEmails } from '../../hooks/useEmails';
import { useEmailStore } from '../../store/emailStore';
import { useAuthStore } from '../../store/authStore';
import { useAuth } from '../../hooks/useAuth';
import { useNavigate } from 'react-router-dom';
import { useViewport } from '../../hooks/useViewport';
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
  const { isMobile, isTablet } = useViewport();

  const [searchFocused, setSearchFocused] = useState(false);
  const [showFilters, setShowFilters] = useState(false);
  const [showAccount, setShowAccount] = useState(false);
  const [mobileSearch, setMobileSearch] = useState(false);
  const searchRef = useRef<HTMLInputElement>(null);

  const [theme, setTheme] = useState<'dark' | 'light'>(() => {
    const saved = localStorage.getItem('theme');
    if (saved === 'dark' || saved === 'light') return saved;
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  });

  useEffect(() => {
    document.documentElement.classList.toggle('dark', theme === 'dark');
    localStorage.setItem('theme', theme);
  }, [theme]);

  // "/" focuses search, Escape closes any open menu.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const el = document.activeElement;
      const typing = el instanceof HTMLInputElement || el instanceof HTMLTextAreaElement;
      if (e.key === '/' && !typing) {
        e.preventDefault();
        searchRef.current?.focus();
      }
      if (e.key === 'Escape') {
        setShowFilters(false);
        setShowAccount(false);
        setMobileSearch(false);
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  const initials = user?.name
    ? user.name.split(' ').map((n: string) => n[0]).join('').slice(0, 2).toUpperCase()
    : 'VL';

  return (
    <header
      style={{
        height: 60,
        background: 'var(--v-ground)',
        borderBottom: '1px solid var(--v-hairline)',
        display: 'flex',
        alignItems: 'center',
        gap: 14,
        padding: '0 14px',
        flexShrink: 0,
        zIndex: 40,
      }}
    >
      {/* Mobile: search takes over the whole bar when open */}
      {isMobile && mobileSearch && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flex: 1, minWidth: 0 }}>
          <div
            style={{
              display: 'flex', alignItems: 'center', gap: 10, flex: 1, minWidth: 0, height: 40,
              background: 'var(--v-panel)', border: '1px solid var(--v-signal)',
              borderRadius: 'var(--v-r-chip)', padding: '0 12px',
            }}
          >
            <Search size={16} style={{ color: 'var(--v-ink-3)', flexShrink: 0 }} />
            <input
              autoFocus
              type="text"
              placeholder="Search mail…"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              style={{
                flex: 1, minWidth: 0, height: '100%', background: 'transparent',
                border: 'none', outline: 'none', fontSize: 16, color: 'var(--v-ink)',
                fontFamily: 'inherit',
              }}
            />
          </div>
          <button
            onClick={() => { setMobileSearch(false); setSearchQuery(''); }}
            className="vbtn vbtn-bare"
            aria-label="Close search"
            style={{ width: 34, padding: 0, flexShrink: 0 }}
          >
            <X size={18} />
          </button>
        </div>
      )}

      {/* Wordmark */}
      <div
        style={{
          display: isMobile && mobileSearch ? 'none' : 'flex',
          alignItems: 'center',
          gap: 8,
          flexShrink: 0,
        }}
      >
        {!isMobile && (
          <button
            onClick={onToggleSidebar}
            className="vbtn vbtn-bare"
            title="Toggle sidebar"
            aria-label="Toggle sidebar"
            style={{ width: 34, padding: 0 }}
          >
            <PanelLeft size={18} />
          </button>
        )}

        <button
          onClick={() => navigate('/dashboard')}
          aria-label="Velocity — go to dashboard"
          style={{
            display: 'flex', alignItems: 'center', gap: 10,
            background: 'none', border: 'none', cursor: 'pointer', padding: '4px 6px',
          }}
        >
          <Mark />
          <span
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'flex-start',
              gap: 3,
              lineHeight: 1,
            }}
          >
            {/* Uppercase at wide tracking reads as a mark rather than a word. */}
            <span
              style={{
                fontSize: 13,
                fontWeight: 600,
                letterSpacing: '0.26em',
                textIndent: '0.26em',
                color: 'var(--v-ink)',
              }}
            >
              VELOCITY
            </span>
            <span
              aria-hidden="true"
              style={{
                width: '100%',
                height: 1,
                background:
                  'linear-gradient(90deg, var(--v-signal) 0%, var(--v-ember) 65%, transparent 100%)',
                opacity: 0.65,
              }}
            />
          </span>
        </button>
      </div>

      {/* Search — full field from tablet up; an icon on mobile */}
      <div
        style={{
          display: isMobile ? 'none' : 'block',
          flex: 1,
          maxWidth: 640,
          position: 'relative',
          minWidth: 0,
        }}
      >
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            height: 40,
            background: 'var(--v-panel)',
            border: `1px solid ${searchFocused ? 'var(--v-signal)' : 'var(--v-hairline)'}`,
            boxShadow: searchFocused ? 'var(--v-glow)' : 'none',
            borderRadius: 'var(--v-r-chip)',
            padding: '0 12px',
            gap: 10,
            transition: 'all var(--v-fast)',
          }}
        >
          <Search size={16} style={{ color: 'var(--v-ink-3)', flexShrink: 0 }} />
          <input
            ref={searchRef}
            id="topbar-search"
            type="text"
            placeholder="Search mail…"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onFocus={() => setSearchFocused(true)}
            onBlur={() => setSearchFocused(false)}
            style={{
              flex: 1, height: '100%', minWidth: 0,
              background: 'transparent', border: 'none', outline: 'none',
              fontSize: 13.5, color: 'var(--v-ink)', fontFamily: 'inherit',
            }}
          />
          {!searchFocused && !searchQuery && (
            <kbd
              style={{
                flexShrink: 0,
                fontSize: 10, fontWeight: 700, color: 'var(--v-ink-3)',
                border: '1px solid var(--v-hairline)', borderRadius: 5,
                padding: '2px 6px', background: 'var(--v-ground-2)',
              }}
            >
              /
            </kbd>
          )}
          <button
            onClick={() => setShowFilters((p) => !p)}
            className="vbtn vbtn-bare"
            aria-expanded={showFilters}
            style={{ height: 28, padding: '0 8px', fontSize: 12, flexShrink: 0 }}
          >
            Filter <ChevronDown size={13} style={{ transform: showFilters ? 'rotate(180deg)' : 'none', transition: 'transform var(--v-fast)' }} />
          </button>
        </div>

        {showFilters && (
          <div
            className="tile animate-slide-down"
            style={{
              position: 'absolute', top: 48, left: 0, right: 0,
              padding: 16, gap: 10, zIndex: 50, boxShadow: 'var(--v-lift-3)',
            }}
          >
            <span className="v-label">Filter by category</span>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
              <button
                className="chip chip-on"
                onClick={() => { setActiveCategory('ALL'); setShowFilters(false); }}
              >
                All
              </button>
              {Object.keys(CAT_COLORS).map((cat) => (
                <button
                  key={cat}
                  className="chip"
                  onClick={() => {
                    setActiveCategory(cat as any);
                    navigate(`/inbox?category=${cat}`);
                    setShowFilters(false);
                  }}
                >
                  <span className="dot" style={{ ['--dot' as string]: CAT_COLORS[cat].color } as React.CSSProperties} />
                  {CAT_COLORS[cat].label}
                </button>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Right cluster */}
      <div
        style={{
          display: isMobile && mobileSearch ? 'none' : 'flex',
          alignItems: 'center',
          gap: 4,
          flexShrink: 0,
          marginLeft: 'auto',
        }}
      >
        {/* Sync state — the label only fits from desktop up; the dot always shows. */}
        <span
          className="v-meta"
          style={{ display: 'inline-flex', alignItems: 'center', gap: 6, marginRight: 4 }}
          title={user?.lastSyncedAt ? `Last synced ${new Date(user.lastSyncedAt).toLocaleString()}` : 'Never synced'}
        >
          {isSyncing ? (
            <>
              <RefreshCw size={12} className="animate-spin" style={{ color: 'var(--v-signal)' }} />
              {!isMobile && !isTablet && 'Syncing'}
            </>
          ) : (
            <>
              <span className="dot" style={{ ['--dot' as string]: 'var(--v-pulse)' } as React.CSSProperties} />
              {!isMobile && !isTablet && (user?.lastSyncedAt ? formatRelative(user.lastSyncedAt) : 'Synced')}
            </>
          )}
        </span>

        {isMobile && (
          <button
            onClick={() => setMobileSearch(true)}
            className="vbtn vbtn-bare"
            aria-label="Search mail"
            style={{ width: 34, padding: 0 }}
          >
            <Search size={17} />
          </button>
        )}

        <button
          onClick={() => setTheme((p) => (p === 'dark' ? 'light' : 'dark'))}
          className="vbtn vbtn-bare"
          title={theme === 'dark' ? 'Switch to light' : 'Switch to dark'}
          aria-label="Toggle theme"
          style={{ width: 34, padding: 0 }}
        >
          {theme === 'dark' ? <Moon size={17} /> : <Sun size={17} />}
        </button>

        <div style={{ position: 'relative' }}>
          <button
            onClick={togglePanel}
            className="vbtn vbtn-bare"
            title="Notifications"
            aria-label="Notifications"
            style={{
              width: 34, padding: 0, position: 'relative',
              background: isPanelOpen ? 'var(--v-panel-2)' : undefined,
            }}
          >
            <Bell size={17} />
            {unreadCount > 0 && (
              <span
                style={{
                  position: 'absolute', top: 5, right: 5,
                  width: 7, height: 7, borderRadius: 999,
                  background: 'var(--v-ember)',
                }}
              />
            )}
          </button>
          <NotificationPanel />
        </div>

        <div style={{ position: 'relative' }}>
          <button
            onClick={() => setShowAccount((p) => !p)}
            aria-expanded={showAccount}
            aria-label="Account menu"
            style={{
              width: 34, height: 34, borderRadius: 10,
              background: 'var(--v-signal)', border: 'none', color: '#fff',
              fontSize: 12, fontWeight: 800, cursor: 'pointer',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}
          >
            {initials}
          </button>

          {showAccount && (
            <div
              className="tile animate-slide-down"
              style={{
                position: 'absolute', top: 42, right: 0, width: 232,
                padding: 0, gap: 0, zIndex: 50, boxShadow: 'var(--v-lift-3)',
              }}
            >
              <div style={{ padding: '14px 16px', borderBottom: '1px solid var(--v-hairline)' }}>
                <p className="v-title truncate">{user?.name || 'User'}</p>
                <p className="v-meta truncate" style={{ marginTop: 2 }}>{user?.email || ''}</p>
              </div>
              <div style={{ padding: 8, display: 'flex', flexDirection: 'column', gap: 2 }}>
                <button
                  onClick={() => { navigate('/settings'); setShowAccount(false); }}
                  className="rail-item"
                  style={{ border: 'none', background: 'transparent', cursor: 'pointer', width: '100%' }}
                >
                  <SettingsIcon size={16} /> Settings
                </button>
                <button
                  onClick={() => { handleLogout(); setShowAccount(false); }}
                  className="rail-item"
                  style={{
                    border: 'none', background: 'transparent', cursor: 'pointer',
                    width: '100%', color: 'var(--v-critical)',
                  }}
                >
                  <LogOut size={16} /> Log out
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};

/**
 * The monogram is the dashboard's own instrument, shrunk: the same 270°
 * gauge arc, a gold V for the needle's rest position, and an ember bead
 * closing the gap at the bottom.
 */
const Mark: React.FC = () => (
  <svg width="30" height="30" viewBox="0 0 32 32" aria-hidden="true" style={{ flexShrink: 0 }}>
    <defs>
      <linearGradient id="velocity-gold" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0%" stopColor="var(--v-signal-dim)" />
        <stop offset="100%" stopColor="var(--v-signal)" />
      </linearGradient>
    </defs>

    {/* gauge ring, open at the bottom */}
    <path
      d="M 7.51 24.49 A 12 12 0 1 1 24.49 24.49"
      fill="none"
      stroke="url(#velocity-gold)"
      strokeWidth="1.6"
      strokeLinecap="round"
      opacity="0.5"
    />

    {/* V — the needle at rest */}
    <path
      d="M 10.4 11.2 L 16 20.6 L 21.6 11.2"
      fill="none"
      stroke="url(#velocity-gold)"
      strokeWidth="3"
      strokeLinecap="round"
      strokeLinejoin="round"
    />

    {/* ember bead closing the arc */}
    <circle cx="16" cy="26.2" r="2" fill="var(--v-ember)" />
  </svg>
);
