import React from 'react';
import { Sidebar } from './Sidebar';
import { TopBar } from './TopBar';
import { MobileBottomNav } from './MobileBottomNav';
import { BottomSheet } from '../common/BottomSheet';
import { useViewport } from '../../hooks/useViewport';
import { useUIStore } from '../../store/uiStore';
import { Plus, Sparkles, Clock } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useMailPrefetch } from '../../hooks/useMailPrefetch';

interface AppShellProps {
  children: React.ReactNode;
  title?: string;
  subtitle?: string;
  actions?: React.ReactNode;
  noScroll?: boolean;
  /** Full-height pane: no padding, child fills main column (inbox, brain, etc.) */
  flush?: boolean;
}

export const AppShell: React.FC<AppShellProps> = ({
  children,
  title,
  subtitle,
  actions,
  noScroll = false,
  flush = false,
}) => {
  const { isMobile } = useViewport();
  const { bottomSheet, closeBottomSheet, openBottomSheet } = useUIStore();
  const navigate = useNavigate();
  useMailPrefetch();

  const hasHeader = !!(title || subtitle || actions);

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        height: '100dvh',
        width: '100%',
        background: 'var(--v-ground-2)',
        color: 'var(--v-ink)',
        overflow: 'hidden',
      }}
    >
      <TopBar />

      <div style={{ display: 'flex', flex: 1, minHeight: 0, minWidth: 0 }}>
        {!isMobile && <Sidebar />}

        <main
          role="main"
          className="v-scroll"
          style={{
            flex: 1,
            minWidth: 0,
            minHeight: 0,
            display: flush ? 'flex' : undefined,
            flexDirection: flush ? 'column' : undefined,
            overflowY: noScroll || flush ? 'hidden' : 'auto',
            overflowX: 'hidden',
          }}
        >
          <div className={flush ? 'shell-inner shell-inner--flush' : 'shell-inner'}>
            {hasHeader && (
              <header className="shell-header" style={flush ? { paddingBottom: 0 } : undefined}>
                <div style={{ minWidth: 0 }}>
                  {title && <h1>{title}</h1>}
                  {subtitle && (
                    <p className="v-body" style={{ marginTop: 6, color: 'var(--v-ink-3)' }}>
                      {subtitle}
                    </p>
                  )}
                </div>
                {actions && (
                  <div style={{ display: 'flex', gap: 8, flexShrink: 0, flexWrap: 'wrap' }}>{actions}</div>
                )}
              </header>
            )}
            {flush ? (
              <div
                className={`shell-flush-body${hasHeader ? ' shell-flush-body--pad-header' : ' shell-flush-body--full'}`}
              >
                {children}
              </div>
            ) : (
              children
            )}
          </div>
        </main>
      </div>

      {isMobile && <MobileBottomNav />}

      {isMobile && (
        <button
          onClick={() => openBottomSheet('QUICK_ACTIONS')}
          aria-label="Quick actions"
          className="shell-quick-fab"
          style={{
            position: 'fixed',
            bottom: 'calc(env(safe-area-inset-bottom) + 78px)',
            right: 16,
            width: 50,
            height: 50,
            borderRadius: 16,
            border: 'none',
            background: 'var(--v-signal)',
            color: 'var(--v-on-signal)',
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
            onClick={() => { closeBottomSheet(); navigate('/brain'); }}
            className="vbtn vbtn-quiet"
            style={{ width: '100%', justifyContent: 'flex-start', height: 48 }}
          >
            <Sparkles size={17} style={{ color: 'var(--v-signal)' }} /> Ask Cortex Brain
          </button>
          <button
            onClick={() => { closeBottomSheet(); navigate('/priority'); }}
            className="vbtn vbtn-quiet"
            style={{ width: '100%', justifyContent: 'flex-start', height: 48 }}
          >
            <Clock size={17} style={{ color: 'var(--v-ember)' }} /> View deadlines
          </button>
        </div>
      </BottomSheet>
    </div>
  );
};
