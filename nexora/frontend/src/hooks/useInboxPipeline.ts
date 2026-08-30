import { useEffect, useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { emailApi } from '../api/emailApi';
import { invalidateAfterGmailSync, runGmailSync } from '../api/gmailSync';
import { queryKeys } from '../api/queryKeys';
import { useAuthStore } from '../store/authStore';
import { resolveSyncChip, type PipelinePhase, type SyncChip } from '../utils/syncChip';

export type { PipelinePhase, SyncChip } from '../utils/syncChip';

/**
 * Dashboard pipeline: kick Gmail sync, then poll sync-status.
 * Status splits: pulling mail → grouping → enriching → ready.
 */
export function useInboxPipeline(autoStart = true) {
  const { user, setLastSyncedAt } = useAuthStore();
  const queryClient = useQueryClient();
  const [phase, setPhase] = useState<PipelinePhase>('idle');
  const [status, setStatus] = useState('');
  const started = useRef(false);
  const wasBusy = useRef(false);
  const awaitingFirstSync = useRef(false);
  const prevUnclassified = useRef<number | null>(null);
  const prevSecondary = useRef<boolean | null>(null);

  const syncMutation = useMutation({
    mutationKey: ['gmail-sync'],
    mutationFn: () => runGmailSync(queryClient),
  });

  const runPipeline = async (force = false) => {
    if (syncMutation.isPending) return;

    try {
      const needsSync = force || !user?.lastSyncedAt;
      if (needsSync) {
        setPhase('syncing');
        setStatus('Pulling your latest Gmail inbox…');
        const syncResult = await syncMutation.mutateAsync();

        if (syncResult.syncMode === 'SKIPPED') {
          setPhase('busy');
          setStatus(syncResult.message || 'Sync already running — showing stored mail.');
          return;
        }

        if (syncResult.syncMode === 'STARTED') {
          awaitingFirstSync.current = true;
          setStatus(syncResult.message || 'Gmail sync started — inbox will appear shortly…');
          return;
        }

        setLastSyncedAt(new Date().toISOString());
      }

      awaitingFirstSync.current = false;
      setPhase('grouped');
      setStatus('Inbox ready — organizing mail in the background…');
    } catch (err: unknown) {
      console.error('Inbox pipeline failed', err);
      awaitingFirstSync.current = false;
      setPhase('error');
      const message = err && typeof err === 'object' && 'response' in err
        ? (err as { response?: { data?: { message?: string } }; message?: string }).response?.data?.message
          ?? (err as { message?: string }).message
        : undefined;
      setStatus(message || 'Gmail sync failed');
    }
  };

  useEffect(() => {
    if (!autoStart || started.current || !user) return;
    started.current = true;
    if (!user.lastSyncedAt) {
      void runPipeline(true);
    } else {
      setPhase('grouped');
      setStatus('Inbox ready.');
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.userId]);

  const pullingMail = phase === 'syncing' || syncMutation.isPending || awaitingFirstSync.current;

  const { data: syncStatus } = useQuery({
    queryKey: queryKeys.syncStatus,
    queryFn: emailApi.getSyncStatus,
    enabled: Boolean(user),
    staleTime: 8_000,
    refetchInterval: (query) => {
      if (pullingMail || awaitingFirstSync.current) return 2000;
      const data = query.state.data;
      if (data?.syncInProgress) return 2500;
      const unclassified = Number(data?.unclassifiedInbox ?? 0);
      // Poll while grouping; do NOT poll forever just because historyId is missing.
      if (unclassified > 0) return 3500;
      return false;
    },
  });

  useEffect(() => {
    if (!awaitingFirstSync.current) return;
    const inbox = Number(syncStatus?.localCounts?.inboxTotal ?? 0);
    if (syncStatus?.lastSyncedAt || inbox > 0) {
      awaitingFirstSync.current = false;
      setLastSyncedAt(syncStatus?.lastSyncedAt || new Date().toISOString());
      setPhase('grouped');
      setStatus('Inbox ready — organizing mail in the background…');
      void invalidateAfterGmailSync(queryClient);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [syncStatus?.lastSyncedAt, syncStatus?.localCounts?.inboxTotal]);

  const unclassified = Number(syncStatus?.unclassifiedInbox ?? 0);
  const secondaryComplete = syncStatus?.secondaryComplete === true;
  const syncInProgress = syncStatus?.syncInProgress === true;
  const hasSyncedMail = Boolean(user?.lastSyncedAt || syncStatus?.lastSyncedAt)
    || Number(syncStatus?.localCounts?.inboxTotal ?? 0) > 0
    || Number(syncStatus?.localCounts?.allStored ?? 0) > 0;

  const classifyingInBackground = hasSyncedMail && unclassified > 0;
  const enrichingInBackground = hasSyncedMail
    && !secondaryComplete
    && unclassified === 0
    && syncInProgress;

  const isPipelineRunning = pullingMail;
  const backgroundBusy = classifyingInBackground || enrichingInBackground || isPipelineRunning || syncInProgress;

  const syncChip: SyncChip = resolveSyncChip({
    phase,
    isPipelineRunning,
    hasSyncedMail,
    syncInProgress,
    classifyingInBackground,
    enrichingInBackground,
  });

  useEffect(() => {
    if (phase === 'error') return;

    if (awaitingFirstSync.current || phase === 'syncing') {
      setStatus(hasSyncedMail
        ? 'Updating inbox from Gmail…'
        : 'Pulling your latest Gmail inbox…');
      return;
    }
    if (classifyingInBackground) {
      setStatus(`${unclassified} messages still being grouped…`);
      return;
    }
    if (enrichingInBackground) {
      setStatus('Finishing full sync — score and drafts warm up next…');
      return;
    }
    if (phase === 'busy' && !syncInProgress) {
      setPhase('grouped');
      setStatus(hasSyncedMail ? 'Inbox ready.' : 'Waiting for mail…');
      return;
    }
    if (hasSyncedMail && unclassified === 0 && !syncInProgress) {
      setStatus('Inbox ready.');
      if (phase === 'idle' || phase === 'busy') setPhase('grouped');
    }
  }, [
    classifyingInBackground,
    enrichingInBackground,
    unclassified,
    secondaryComplete,
    phase,
    syncInProgress,
    hasSyncedMail,
  ]);

  // Refresh lists as categories fill in.
  useEffect(() => {
    if (prevUnclassified.current == null) {
      prevUnclassified.current = unclassified;
      return;
    }
    if (unclassified !== prevUnclassified.current) {
      void invalidateAfterGmailSync(queryClient);
    }
    prevUnclassified.current = unclassified;
  }, [unclassified, queryClient]);

  // Refresh when secondary enrichment finishes (historyId lands).
  useEffect(() => {
    if (prevSecondary.current == null) {
      prevSecondary.current = secondaryComplete;
      return;
    }
    if (!prevSecondary.current && secondaryComplete) {
      void invalidateAfterGmailSync(queryClient);
    }
    prevSecondary.current = secondaryComplete;
  }, [secondaryComplete, queryClient]);

  useEffect(() => {
    if (backgroundBusy) {
      wasBusy.current = true;
      return;
    }
    if (!wasBusy.current) return;
    wasBusy.current = false;
    void invalidateAfterGmailSync(queryClient);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [backgroundBusy]);

  const categoryCounts = syncStatus?.categoryGroups ?? {};
  const inboxUnread = Number(syncStatus?.gmailCounts?.inboxUnread ?? syncStatus?.localCounts?.inboxUnread ?? 0);

  return {
    phase,
    status,
    syncChip,
    runPipeline,
    syncStatus,
    unclassified,
    secondaryComplete,
    categoryCounts,
    inboxUnread,
    labelCounts: undefined as undefined,
    isPipelineRunning,
    isClassifyingInBackground: classifyingInBackground,
    isEnrichingInBackground: enrichingInBackground,
    isBackgroundBusy: backgroundBusy,
    hasSyncedMail,
  };
}
