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
  noScroll?: boolean;
}

export const AppShell: React.FC<AppShellProps> = ({
  children,
  title,
  subtitle,
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
        height: '100vh',
        width: '100vw',
        background: 'var(--bg)',
        overflow: 'hidden',
        color: 'var(--text-1)',
      }}
    >
      {/* Live Region for Screen Reader Accessibility */}
      <div className="sr-only" aria-live="polite" aria-atomic="true">
        {isSyncing ? 'Syncing Gmail inbox...' : 'Gmail synced.'}
      </div>

      {/* Top Bar */}
      <TopBar onToggleSidebar={toggleSidebar} />

      <div style={{ display: 'flex', flex: 1, overflow: 'hidden', position: 'relative' }}>
        {/* Desktop / Tablet Sidebar */}
        {!isMobile && (
          <Sidebar collapsed={isTablet || sidebarCollapsed} />
        )}

        {/* Main Workspace Area */}
        <main
          role="main"
          style={{
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
            overflowY: noScroll ? 'hidden' : 'auto',
            padding: isMobile
              ? '16px 16px 80px'
              : isTablet
              ? '24px'
              : 'var(--spacing-page)',
            position: 'relative',
            maxWidth: 1600,
            margin: '0 auto',
            width: '100%',
          }}
        >
          {(title || subtitle) && (
            <div style={{ marginBottom: isMobile ? 16 : 24 }}>
              {title && (
                <h1
                  style={{
                    fontSize: isMobile ? 22 : 28,
                    fontWeight: 800,
                    color: 'var(--text-1)',
                    margin: 0,
                    fontFamily: 'Google Sans, Roboto, sans-serif',
                    letterSpacing: '-0.02em',
                  }}
                >
                  {title}
                </h1>
              )}
              {subtitle && (
                <p style={{ fontSize: 13, color: 'var(--text-2)', margin: '4px 0 0' }}>
                  {subtitle}
                </p>
              )}
            </div>
          )}

          {children}
        </main>
      </div>

      {/* Mobile Bottom Navigation */}
      {isMobile && <MobileBottomNav />}

      {/* Mobile Floating Action Button (FAB) */}
      {isMobile && (
        <button
          onClick={() => openBottomSheet('QUICK_ACTIONS')}
          className="btn-ember"
          aria-label="Quick Actions"
          style={{
            position: 'fixed',
            bottom: 80,
            right: 16,
            width: 52,
            height: 52,
            borderRadius: '50%',
            padding: 0,
            justifyContent: 'center',
            zIndex: 40,
          }}
        >
          <Plus size={24} />
        </button>
      )}

      {/* Mobile Bottom Sheet Modal */}
      <BottomSheet
        isOpen={bottomSheet.isOpen}
        onClose={closeBottomSheet}
        title={bottomSheet.type === 'QUICK_ACTIONS' ? 'Quick Actions' : 'Velocity Options'}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <button
            onClick={() => { closeBottomSheet(); sync(); }}
            className="btn-outline"
            style={{ width: '100%', justifyContent: 'flex-start', height: 48, fontSize: 14 }}
          >
            <RefreshCw size={18} style={{ color: 'var(--ember)' }} /> Sync Gmail Inbox
          </button>
          <button
            onClick={() => { closeBottomSheet(); navigate('/brain'); }}
            className="btn-outline"
            style={{ width: '100%', justifyContent: 'flex-start', height: 48, fontSize: 14 }}
          >
            <Sparkles size={18} style={{ color: 'var(--violet)' }} /> Ask Velocity Brain
          </button>
          <button
            onClick={() => { closeBottomSheet(); navigate('/inbox?filter=deadlines'); }}
            className="btn-outline"
            style={{ width: '100%', justifyContent: 'flex-start', height: 48, fontSize: 14 }}
          >
            <Clock size={18} style={{ color: 'var(--warn)' }} /> View Deadlines
          </button>
        </div>
      </BottomSheet>
    </div>
  );
};
