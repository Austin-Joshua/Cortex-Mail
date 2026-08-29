import { useEffect, useRef, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { emailApi } from '../api/emailApi';
import { authApi } from '../api/authApi';
import { useAuthStore } from '../store/authStore';
import { useEmails } from './useEmails';

export type PipelinePhase =
  | 'idle'
  | 'syncing'
  | 'inbox_ready'
  | 'scoring'
  | 'extracting'
  | 'grouped'
  | 'busy'
  | 'error';

/** Dashboard pipeline: sync only when never synced, then classify in background. */
export function useInboxPipeline(autoStart = true) {
  const { user, setUser, setLastSyncedAt } = useAuthStore();
  const queryClient = useQueryClient();
  const emailsState = useEmails(0, 80);
  const [phase, setPhase] = useState<PipelinePhase>('idle');
  const [status, setStatus] = useState('');
  const [classified, setClassified] = useState(0);
  const started = useRef(false);
  const classifyBgStarted = useRef(false);

  const syncMutation = useMutation({
    mutationFn: emailApi.syncEmails,
  });

  const classifyMutation = useMutation({
    mutationFn: emailApi.classifyInbox,
  });

  const refreshUser = async () => {
    try {
      const updatedUser = await authApi.getCurrentUser();
      setUser({
        userId: updatedUser.userId,
        email: updatedUser.email,
        name: updatedUser.name,
        profilePictureUrl: updatedUser.profilePictureUrl ?? undefined,
        userRole: updatedUser.userRole,
        onboardingComplete: updatedUser.onboardingComplete,
        calendarSyncEnabled: updatedUser.calendarSyncEnabled ?? true,
        lastSyncedAt: updatedUser.lastSyncedAt,
      });
    } catch {
      /* ignore */
    }
  };

  const runClassifyStep = async () => {
    setPhase('extracting');
    setStatus('Separating mail by source and content…');
    const classifyResult = await classifyMutation.mutateAsync({});
    setClassified(typeof classifyResult.classified === 'number' ? classifyResult.classified : 0);

    queryClient.invalidateQueries({ queryKey: ['emails'] });
    queryClient.invalidateQueries({ queryKey: ['email-categories'] });
    queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
    queryClient.invalidateQueries({ queryKey: ['sync-status'] });

    setPhase('grouped');
    setStatus(
      `Inbox ready · ${typeof classifyResult.classified === 'number' ? classifyResult.classified : 0} mails separated into groups.`,
    );
  };

  const runPipeline = async (force = false) => {
    if (syncMutation.isPending || classifyMutation.isPending) return;

    try {
      const needsSync = force || !user?.lastSyncedAt;
      if (needsSync) {
        setPhase('syncing');
        setStatus('Extracting your real Gmail inbox…');
        const syncResult = await syncMutation.mutateAsync();

        if (syncResult.syncMode === 'SKIPPED') {
          if (syncResult.labelCounts) {
            queryClient.setQueryData(['gmail-label-counts'], syncResult.labelCounts);
          }
          queryClient.invalidateQueries({ queryKey: ['emails'] });
          queryClient.invalidateQueries({ queryKey: ['sync-status'] });
          setStatus(syncResult.message || 'Background sync already running — classifying local mail.');
          if (!force) {
            setPhase('grouped');
            return;
          }
        } else {
          if (syncResult.labelCounts) {
            queryClient.setQueryData(['gmail-label-counts'], syncResult.labelCounts);
          }
          setLastSyncedAt(new Date().toISOString());
          await refreshUser();
          queryClient.invalidateQueries({ queryKey: ['emails'] });
          queryClient.invalidateQueries({ queryKey: ['email-drafts'] });
          queryClient.invalidateQueries({ queryKey: ['email-archived'] });
          queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
          queryClient.invalidateQueries({ queryKey: ['gmail-label-counts'] });
          queryClient.invalidateQueries({ queryKey: ['sync-status'] });
        }
      }

      setPhase('inbox_ready');
      setStatus('Inbox loaded. Computing Cortex Score…');
      setPhase('scoring');
      queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });

      await runClassifyStep();
    } catch (err: unknown) {
      console.error('Inbox pipeline failed', err);
      setPhase('error');
      const message = err && typeof err === 'object' && 'response' in err
        ? (err as { response?: { data?: { message?: string } }; message?: string }).response?.data?.message
          ?? (err as { message?: string }).message
        : undefined;
      setStatus(message || 'Gmail pipeline failed');
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

  useEffect(() => {
    if (!user?.lastSyncedAt || classifyBgStarted.current || classifyMutation.isPending) return;
    const unclassified = Number(emailsState.categoryCounts?.UNCATEGORIZED ?? 0);
    if (unclassified <= 0) return;
    classifyBgStarted.current = true;
    void (async () => {
      try {
        await runClassifyStep();
      } catch {
        classifyBgStarted.current = false;
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.lastSyncedAt, emailsState.categoryCounts?.UNCATEGORIZED]);

  return {
    ...emailsState,
    phase,
    status,
    classified,
    runPipeline,
    isPipelineRunning:
      phase === 'syncing' || phase === 'extracting' || phase === 'scoring'
      || syncMutation.isPending || classifyMutation.isPending,
  };
}
