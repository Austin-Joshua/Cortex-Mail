import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useInboxPipeline } from '../../hooks/useInboxPipeline';
import type { PipelinePhase, SyncChip } from '../../utils/syncChip';

function chipColor(chip: SyncChip): string {
  if (chip === 'error') return 'var(--color-danger)';
  if (chip === 'syncing' || chip === 'classifying' || chip === 'enriching' || chip === 'busy') {
    return 'var(--color-cortex)';
  }
  if (chip === 'synced') return 'var(--color-success)';
  return 'var(--color-text-primary)';
}

export interface SyncPipelineBannerProps {
  syncChip: SyncChip;
  phase: PipelinePhase;
  status: string;
  lastSyncedAt?: string | null;
  isBackgroundBusy: boolean;
  onSync: () => void;
  showOpenInbox?: boolean;
}

/** Presentational sync chip + status banner — shared across mailbox pages. */
export const SyncPipelineBanner: React.FC<SyncPipelineBannerProps> = ({
  syncChip,
  phase,
  status,
  lastSyncedAt,
  isBackgroundBusy,
  onSync,
  showOpenInbox = false,
}) => {
  const navigate = useNavigate();

  const lastSyncedLabel = (() => {
    if (!lastSyncedAt) return null;
    try {
      return new Date(lastSyncedAt).toLocaleString();
    } catch {
      return String(lastSyncedAt);
    }
  })();

  const showBanner = Boolean(
    status && (isBackgroundBusy || phase === 'error' || phase === 'grouped' || phase === 'busy' || phase === 'syncing'),
  );

  return (
    <>
      <div className="sync-toolbar">
        <span>
          Sync:{' '}
          <strong style={{ color: chipColor(syncChip) }}>{syncChip}</strong>
          {lastSyncedLabel ? ` · last synced ${lastSyncedLabel}` : ''}
        </span>
        <button type="button" className="vbtn vbtn-bare" style={{ height: 28 }} onClick={onSync}>
          Sync now
        </button>
      </div>

      {showBanner && (
        <div
          className="status-banner"
          style={{
            background: phase === 'error'
              ? 'var(--color-danger-soft)'
              : syncChip === 'classifying' || syncChip === 'enriching' || syncChip === 'busy' || syncChip === 'syncing'
                ? 'var(--color-cortex-soft)'
                : 'var(--color-surface-elevated)',
            color: phase === 'error'
              ? 'var(--color-danger)'
              : syncChip === 'classifying' || syncChip === 'enriching' || syncChip === 'busy' || syncChip === 'syncing'
                ? 'var(--color-cortex-light)'
                : 'var(--color-text-secondary)',
          }}
        >
          <span>{status}</span>
          {phase === 'error' && (
            <button type="button" className="vbtn vbtn-quiet" onClick={onSync}>
              Retry
            </button>
          )}
          {showOpenInbox && (syncChip === 'synced' || phase === 'grouped') && !isBackgroundBusy && (
            <button type="button" className="vbtn vbtn-bare" onClick={() => navigate('/inbox')}>
              Open inbox
            </button>
          )}
        </div>
      )}
    </>
  );
};

/** Hook-connected banner for pages that only need sync chrome. */
export const ConnectedSyncPipelineBanner: React.FC<{
  autoStart?: boolean;
  showOpenInbox?: boolean;
}> = ({ autoStart = false, showOpenInbox = false }) => {
  const pipeline = useInboxPipeline(autoStart);
  return (
    <SyncPipelineBanner
      syncChip={pipeline.syncChip}
      phase={pipeline.phase}
      status={pipeline.status}
      lastSyncedAt={pipeline.syncStatus?.lastSyncedAt}
      isBackgroundBusy={pipeline.isBackgroundBusy}
      onSync={() => void pipeline.runPipeline(true)}
      showOpenInbox={showOpenInbox}
    />
  );
};
