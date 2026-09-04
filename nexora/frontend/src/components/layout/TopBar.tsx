import React, { useState, useEffect, useRef } from 'react';
import {
  Search, LogOut, X, Bell, RefreshCw,
  Settings as SettingsIcon, ChevronDown,
} from 'lucide-react';
import { useEmailStore } from '../../store/emailStore';
import { useAuthStore } from '../../store/authStore';
import { useAuth } from '../../hooks/useAuth';
import { useEmailSync } from '../../hooks/useEmailSync';
import { useNotificationStore } from '../../store/notificationStore';
import { NotificationPanel } from '../notifications/NotificationPanel';
import { useNavigate } from 'react-router-dom';
import { useViewport } from '../../hooks/useViewport';
import { useQuery } from '@tanstack/react-query';
import { notificationApi } from '../../api/notificationApi';
import { queryKeys } from '../../api/queryKeys';
import { CAT_COLORS } from '../../utils/catColors';
import { BrandLogo } from '../common/BrandLogo';
import { useUIStore } from '../../store/uiStore';

export const TopBar: React.FC = () => {
  const { setSearchQuery, searchQuery, setActiveCategory } = useEmailStore();
  const { user, isAuthenticated } = useAuthStore();
  const { handleLogout } = useAuth();
  const { sync, isSyncing, syncError, lastSyncMode } = useEmailSync();
  const { unreadCount, setUnreadCount, togglePanel, isPanelOpen } = useNotificationStore();
  const { pageTitle } = useUIStore();
  const navigate = useNavigate();
  const { isMobile } = useViewport();

  const [searchFocused, setSearchFocused] = useState(false);
  const [showFilters, setShowFilters] = useState(false);
  const [showAccount, setShowAccount] = useState(false);
  const [mobileSearch, setMobileSearch] = useState(false);
  const searchRef = useRef<HTMLInputElement>(null);

  const { data: notifBadge } = useQuery({
    queryKey: queryKeys.notificationsUnread,
    queryFn: notificationApi.getUnreadCount,
    staleTime: 60_000,
    refetchInterval: 120_000,
  });

  useEffect(() => {
    if (typeof notifBadge === 'number') setUnreadCount(notifBadge);
  }, [notifBadge, setUnreadCount]);

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

  const goHome = () => {
    if (isAuthenticated) {
      navigate('/dashboard');
      return;
    }
    navigate('/');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const goInboxForSearch = (value?: string) => {
    if (value !== undefined) setSearchQuery(value);
    if (!window.location.pathname.startsWith('/inbox')) {
      navigate('/inbox');
    }
  };

  const initials = user?.name
    ? user.name.split(/\s+/).filter(Boolean).map((n: string) => n[0]).join('').slice(0, 2).toUpperCase()
    : 'CM';

  const showMobileTitle = isMobile && !!pageTitle && !mobileSearch;

  return (
    <header
      className="app-topbar"
      style={{
        background: 'var(--v-panel)',
        borderBottom: '1px solid var(--v-hairline)',
        display: 'flex',
        alignItems: 'center',
        flexShrink: 0,
        zIndex: 40,
        boxShadow: 'var(--v-lift-1)',
        gap: isMobile ? 12 : 24,
      }}
    >
      {showMobileTitle && (
        <h1 className="app-topbar-title" title={pageTitle ?? undefined}>
          {pageTitle}
        </h1>
      )}

      {isMobile && mobileSearch && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flex: 1, minWidth: 0 }}>
          <div
            style={{
              display: 'flex', alignItems: 'center', gap: 10, flex: 1, minWidth: 0, height: 42,
              background: 'var(--v-ground-2)', border: '1px solid var(--v-signal)',
              borderRadius: 'var(--v-r-chip)', padding: '0 12px',
            }}
          >
            <Search size={16} style={{ color: 'var(--v-ink-3)', flexShrink: 0 }} />
            <input
              autoFocus
              type="text"
              placeholder="Search mail…"
              value={searchQuery}
              onChange={(e) => goInboxForSearch(e.target.value)}
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

      <div
        style={{
          display: isMobile && mobileSearch ? 'none' : 'flex',
          alignItems: 'center',
          gap: isMobile ? 12 : 28,
          flex: 1,
          minWidth: 0,
        }}
      >
        <BrandLogo
          size={isMobile ? 28 : 34}
          textSize={isMobile ? 13 : 15}
          showText={!isMobile}
          onClick={goHome}
          ariaLabel={isAuthenticated ? 'Cortex Mail — dashboard' : 'Cortex Mail — home'}
        />

        {!isMobile && (
          <div
            style={{
              flex: 1,
              maxWidth: 720,
              position: 'relative',
              minWidth: 0,
            }}
          >
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                height: 42,
                background: 'var(--v-ground-2)',
                border: `1px solid ${searchFocused ? 'var(--v-signal)' : 'var(--v-hairline)'}`,
                boxShadow: searchFocused ? 'var(--v-glow)' : 'none',
                borderRadius: 'var(--v-r-chip)',
                padding: '0 14px',
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
                onChange={(e) => goInboxForSearch(e.target.value)}
                onFocus={() => {
                  setSearchFocused(true);
                  goInboxForSearch();
                }}
                onBlur={() => setSearchFocused(false)}
                style={{
                  flex: 1, height: '100%', minWidth: 0,
                  background: 'transparent', border: 'none', outline: 'none',
                  fontSize: 14, color: 'var(--v-ink)', fontFamily: 'inherit',
                }}
              />
              {!searchFocused && !searchQuery && (
                <kbd
                  style={{
                    flexShrink: 0,
                    fontSize: 10, fontWeight: 700, color: 'var(--v-ink-3)',
                    border: '1px solid var(--v-hairline)', borderRadius: 5,
                    padding: '2px 6px', background: 'var(--v-panel)',
                  }}
                >
                  /
                </kbd>
              )}
              <button
                onClick={() => setShowFilters((p) => !p)}
                className="vbtn vbtn-bare"
                aria-expanded={showFilters}
                style={{ height: 28, padding: '0 8px', fontSize: 12, flexShrink: 0, color: 'var(--v-signal)' }}
              >
                Filter <ChevronDown size={13} style={{ transform: showFilters ? 'rotate(180deg)' : 'none', transition: 'transform var(--v-fast)' }} />
              </button>
            </div>

            {showFilters && (
              <div
                className="tile animate-slide-down"
                style={{
                  position: 'absolute', top: 50, left: 0, right: 0,
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
                      <span className="dot" style={{ ['--dot' as string]: CAT_COLORS[cat].text } as React.CSSProperties} />
                      {CAT_COLORS[cat].label}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      <div
        style={{
          display: isMobile && mobileSearch ? 'none' : 'flex',
          alignItems: 'center',
          gap: 6,
          flexShrink: 0,
          marginLeft: isMobile ? 'auto' : 0,
        }}
      >
        {isMobile && (
          <button
            onClick={() => {
              setMobileSearch(true);
              goInboxForSearch();
            }}
            className="vbtn vbtn-bare"
            aria-label="Search mail"
            style={{ width: 36, height: 36, padding: 0 }}
          >
            <Search size={17} />
          </button>
        )}

        <div style={{ position: 'relative' }}>
          <button
            type="button"
            onClick={() => {
              setShowAccount(false);
              togglePanel();
            }}
            className="vbtn vbtn-bare"
            aria-label="Notifications"
            aria-expanded={isPanelOpen}
            title="Notifications"
            style={{ width: 36, height: 36, padding: 0, color: 'var(--v-ink-2)', position: 'relative' }}
          >
            <Bell size={18} style={{ display: 'block' }} />
            {unreadCount > 0 && (
              <span
                style={{
                  position: 'absolute',
                  top: 4,
                  right: 4,
                  minWidth: 16,
                  height: 16,
                  padding: '0 4px',
                  borderRadius: 999,
                  background: 'var(--v-red)',
                  color: '#fff',
                  fontSize: 9,
                  fontWeight: 800,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  lineHeight: 1,
                }}
              >
                {unreadCount > 9 ? '9+' : unreadCount}
              </span>
            )}
          </button>
          <NotificationPanel />
        </div>

        <button
          type="button"
          onClick={() => sync()}
          disabled={isSyncing}
          className="vbtn vbtn-bare"
          aria-label={isSyncing ? 'Syncing Gmail' : syncError ? 'Gmail sync failed' : 'Sync Gmail'}
          title={
            isSyncing
              ? 'Syncing…'
              : syncError
                ? 'Gmail sync failed — click to retry'
                : lastSyncMode === 'SKIPPED'
                  ? 'Sync already running — showing stored mail'
                  : 'Sync mail from Gmail'
          }
          style={{
            width: 36,
            height: 36,
            padding: 0,
            color: syncError
              ? 'var(--color-danger)'
              : isSyncing ? 'var(--color-cortex-light)' : 'var(--color-text-secondary)',
          }}
        >
          <RefreshCw
            size={18}
            className={isSyncing ? 'animate-spin' : undefined}
            style={{ display: 'block' }}
          />
        </button>

        <div style={{ position: 'relative', marginLeft: 2 }}>
          <button
            type="button"
            className="app-avatar-btn"
            onClick={() => {
              if (isPanelOpen) togglePanel();
              setShowAccount((p) => !p);
            }}
            aria-expanded={showAccount}
            aria-label="Account menu"
          >
            {user?.profilePictureUrl ? (
              <img src={user.profilePictureUrl} alt="" className="app-avatar-img" />
            ) : (
              <span className="app-avatar-initials">{initials}</span>
            )}
          </button>

          {showAccount && (
            <div
              className="tile animate-slide-down"
              style={{
                position: 'absolute', top: 44, right: 0, width: 232,
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
                  type="button"
                  onClick={() => {
                    setShowAccount(false);
                    handleLogout();
                  }}
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
