export type PipelinePhase =
  | 'idle'
  | 'syncing'
  | 'grouped'
  | 'busy'
  | 'error';

/** User-facing sync chip — never call classifying/enriching "syncing". */
export type SyncChip =
  | 'idle'
  | 'syncing'
  | 'classifying'
  | 'enriching'
  | 'busy'
  | 'synced'
  | 'error';

export interface SyncChipInput {
  phase: PipelinePhase;
  isPipelineRunning: boolean;
  hasSyncedMail: boolean;
  syncInProgress: boolean;
  classifyingInBackground: boolean;
  enrichingInBackground: boolean;
}

/** Pure chip resolver — kept separate from the hook for unit tests. */
export function resolveSyncChip(input: SyncChipInput): SyncChip {
  const {
    phase,
    isPipelineRunning,
    hasSyncedMail,
    syncInProgress,
    classifyingInBackground,
    enrichingInBackground,
  } = input;

  if (phase === 'error') return 'error';
  if (isPipelineRunning) return 'syncing';
  if (phase === 'busy' && syncInProgress) return 'busy';
  if (classifyingInBackground) return 'classifying';
  if (enrichingInBackground) return 'enriching';
  if (hasSyncedMail) return 'synced';
  return 'idle';
}
