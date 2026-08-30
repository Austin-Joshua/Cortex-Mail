import { useMutation, useQueryClient } from '@tanstack/react-query';
import { runGmailSync } from '../api/gmailSync';
import { useAuthStore } from '../store/authStore';

/**
 * Shared Gmail sync control for TopBar (and any non-pipeline caller).
 * Uses the same invalidation path as the dashboard pipeline — no /me write.
 */
export function useEmailSync() {
  const queryClient = useQueryClient();
  const setLastSyncedAt = useAuthStore((s) => s.setLastSyncedAt);

  const syncMutation = useMutation({
    mutationKey: ['gmail-sync'],
    mutationFn: () => runGmailSync(queryClient),
    onSuccess: (result) => {
      if (result.syncMode === 'SKIPPED' || result.syncMode === 'STARTED') {
        return;
      }
      setLastSyncedAt(new Date().toISOString());
    },
    onError: (error: unknown) => {
      console.error('Gmail sync error:', error);
    },
  });

  return {
    sync: syncMutation.mutate,
    isSyncing: syncMutation.isPending,
    syncError: syncMutation.error,
    lastSyncMode: syncMutation.data?.syncMode,
  };
}
