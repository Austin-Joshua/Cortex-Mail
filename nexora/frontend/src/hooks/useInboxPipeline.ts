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
  | 'error';

/**
 * Login → Sync real Gmail inbox → show score → extract/separate by source+content → group.
 */
export function useInboxPipeline(autoStart = true) {
  const { user, setUser, setLastSyncedAt } = useAuthStore();
  const queryClient = useQueryClient();
  const emailsState = useEmails(0, 500);
  const [phase, setPhase] = useState<PipelinePhase>('idle');
  const [status, setStatus] = useState('');
  const [classified, setClassified] = useState(0);
  const started = useRef(false);

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

  const runPipeline = async (force = false) => {
    if (syncMutation.isPending || classifyMutation.isPending) return;

    try {
      const needsSync = force || !user?.lastSyncedAt;
      if (needsSync) {
        setPhase('syncing');
        setStatus('Extracting your real Gmail inbox…');
        const syncResult = await syncMutation.mutateAsync();
        if (syncResult.labelCounts) {
          queryClient.setQueryData(['gmail-label-counts'], syncResult.labelCounts);
        }
        setLastSyncedAt(new Date().toISOString());
        await refreshUser();
        await queryClient.invalidateQueries({ queryKey: ['emails'] });
        await queryClient.invalidateQueries({ queryKey: ['email-drafts'] });
        await queryClient.invalidateQueries({ queryKey: ['email-archived'] });
        await queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
        await queryClient.invalidateQueries({ queryKey: ['gmail-label-counts'] });
        await queryClient.invalidateQueries({ queryKey: ['sync-status'] });
      }

      setPhase('inbox_ready');
      setStatus('Inbox loaded from Gmail. Computing Cortex Score…');
      setPhase('scoring');
      await queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });

      setPhase('extracting');
      setStatus('Separating mail by source and content…');
      const classifyResult = await classifyMutation.mutateAsync();
      setClassified(typeof classifyResult.classified === 'number' ? classifyResult.classified : 0);

      await queryClient.invalidateQueries({ queryKey: ['emails'] });
      await queryClient.invalidateQueries({ queryKey: ['email-categories'] });
      await queryClient.invalidateQueries({ queryKey: ['email-drafts'] });
      await queryClient.invalidateQueries({ queryKey: ['email-archived'] });
      await queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
      await queryClient.invalidateQueries({ queryKey: ['sync-status'] });

      setPhase('grouped');
      setStatus(
        `Inbox ready · ${typeof classifyResult.classified === 'number' ? classifyResult.classified : 0} mails separated into groups.`,
      );
    } catch (err: any) {
      console.error('Inbox pipeline failed', err);
      setPhase('error');
      setStatus(err?.response?.data?.message || err?.message || 'Gmail pipeline failed');
    }
  };

  useEffect(() => {
    if (!autoStart || started.current) return;
    if (!user) return;
    started.current = true;
    // First visit this session: always pull fresh Gmail, then score, then group.
    const key = `cortex-pipeline-v3-${user.userId}`;
    const force = !sessionStorage.getItem(key);
    if (force) sessionStorage.setItem(key, '1');
    void runPipeline(force);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.userId]);

  return {
    ...emailsState,
    phase,
    status,
    classified,
    runPipeline,
    isPipelineRunning:
      phase === 'syncing' || phase === 'extracting' || phase === 'scoring' || syncMutation.isPending,
  };
}
