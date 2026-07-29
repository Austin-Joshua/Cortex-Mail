import React from 'react';
import { Sidebar } from './Sidebar';
import { TopBar } from './TopBar';
import { MobileBottomNav } from './MobileBottomNav';
import { BottomSheet } from '../common/BottomSheet';
import { useViewport } from '../../hooks/useViewport';
import { useUIStore } from '../../store/uiStore';
import { Plus, RefreshCw, Sparkles, Clock } from 'lucide-react';
import { useEmails } from '../../hooks/useEmails';
import { useNavigate } from 'react-router-dom';

interface AppShellProps {
  children: React.ReactNode;
  title?: string;
  subtitle?: string;
  actions?: React.ReactNode;
  noScroll?: boolean;
}

export const AppShell: React.FC<AppShellProps> = ({
  children,
  title,
  subtitle,
  actions,
  noScroll = false,
}) => {
  const { isMobile, isTablet } = useViewport();
  const { sidebarCollapsed, toggleSidebar, bottomSheet, closeBottomSheet, openBottomSheet } = useUIStore();
  const { sync, isSyncing } = useEmails();
  const navigate = useNavigate();

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        height: '100dvh',
        width: '100%',
        background: 'var(--v-ground)',
        color: 'var(--v-ink)',
        overflow: 'hidden',
      }}
    >
      <div className="sr-only" aria-live="polite" aria-atomic="true">
        {isSyncing ? 'Syncing Gmail inbox' : 'Gmail synced'}
      </div>

      <TopBar onToggleSidebar={toggleSidebar} />

      <div style={{ display: 'flex', flex: 1, minHeight: 0 }}>
        {!isMobile && <Sidebar collapsed={isTablet || sidebarCollapsed} />}

        <main
          role="main"
          className="v-scroll"
          style={{
            flex: 1,
            minWidth: 0,
            overflowY: noScroll ? 'hidden' : 'auto',
            overflowX: 'hidden',
          }}
        >
          <div
            style={{
              maxWidth: 1560,
              margin: '0 auto',
              width: '100%',
              padding: isMobile ? '16px 14px 96px' : isTablet ? '22px 22px 32px' : '26px 28px 40px',
            }}
          >
            {(title || subtitle || actions) && (
              <header
                style={{
                  display: 'flex',
                  alignItems: 'flex-end',
                  justifyContent: 'space-between',
                  gap: 16,
                  flexWrap: 'wrap',
                  marginBottom: isMobile ? 16 : 22,
                }}
              >
                <div style={{ minWidth: 0 }}>
                  {title && (
                    <h1
                      style={{
                        fontSize: isMobile ? 24 : 30,
                        fontWeight: 800,
                        letterSpacing: '-0.035em',
                        color: 'var(--v-ink)',
                        margin: 0,
                        lineHeight: 1.1,
                      }}
                    >
                      {title}
                    </h1>
                  )}
                  {subtitle && (
                    <p className="v-body" style={{ marginTop: 6, color: 'var(--v-ink-3)' }}>
                      {subtitle}
                    </p>
                  )}
                </div>
                {actions && (
                  <div style={{ display: 'flex', gap: 8, flexShrink: 0 }}>{actions}</div>
                )}
              </header>
            )}

            {children}
          </div>
        </main>
      </div>

      {isMobile && <MobileBottomNav />}

      {isMobile && (
        <button
          onClick={() => openBottomSheet('QUICK_ACTIONS')}
          aria-label="Quick actions"
          style={{
            position: 'fixed',
            bottom: 'calc(env(safe-area-inset-bottom) + 78px)',
            right: 16,
            width: 50,
            height: 50,
            borderRadius: 16,
            border: 'none',
            background: 'var(--v-amber)',
            color: '#150D00',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: 'var(--v-lift-3)',
            cursor: 'pointer',
            zIndex: 45,
          }}
        >
          <Plus size={22} />
        </button>
      )}

      <BottomSheet
        isOpen={bottomSheet.isOpen}
        onClose={closeBottomSheet}
        title={bottomSheet.type === 'QUICK_ACTIONS' ? 'Quick actions' : 'Options'}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          <button
            onClick={() => { closeBottomSheet(); sync(); }}
            className="vbtn vbtn-quiet"
            style={{ width: '100%', justifyContent: 'flex-start', height: 48 }}
          >
            <RefreshCw size={17} style={{ color: 'var(--v-signal)' }} /> Sync Gmail inbox
          </button>
          <button
            onClick={() => { closeBottomSheet(); navigate('/brain'); }}
            className="vbtn vbtn-quiet"
            style={{ width: '100%', justifyContent: 'flex-start', height: 48 }}
          >
            <Sparkles size={17} style={{ color: 'var(--v-signal)' }} /> Ask Velocity Brain
          </button>
          <button
            onClick={() => { closeBottomSheet(); navigate('/priority'); }}
            className="vbtn vbtn-quiet"
            style={{ width: '100%', justifyContent: 'flex-start', height: 48 }}
          >
            <Clock size={17} style={{ color: 'var(--v-amber)' }} /> View deadlines
          </button>
        </div>
      </BottomSheet>
    </div>
  );
};
