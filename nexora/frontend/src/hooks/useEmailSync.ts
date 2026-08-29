import { useMutation, useQueryClient } from '@tanstack/react-query';
import { emailApi } from '../api/emailApi';
import { authApi } from '../api/authApi';
import { useAuthStore } from '../store/authStore';

/** Lightweight sync control — no inbox list fetch. */
export function useEmailSync() {
  const queryClient = useQueryClient();
  const { setLastSyncedAt, setUser } = useAuthStore();

  const syncMutation = useMutation({
    mutationFn: emailApi.syncEmails,
    onSuccess: async (result) => {
      if (result.syncMode === 'SKIPPED') {
        if (result.labelCounts) {
          queryClient.setQueryData(['gmail-label-counts'], result.labelCounts);
        }
        queryClient.invalidateQueries({ queryKey: ['emails'] });
        queryClient.invalidateQueries({ queryKey: ['sync-status'] });
        return;
      }

      queryClient.invalidateQueries({ queryKey: ['emails'] });
      queryClient.invalidateQueries({ queryKey: ['email-categories'] });
      queryClient.invalidateQueries({ queryKey: ['gmail-label-counts'] });
      queryClient.invalidateQueries({ queryKey: ['email-drafts'] });
      queryClient.invalidateQueries({ queryKey: ['email-archived'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
      queryClient.invalidateQueries({ queryKey: ['sync-status'] });

      if (result.labelCounts) {
        queryClient.setQueryData(['gmail-label-counts'], result.labelCounts);
      }
      setLastSyncedAt(new Date().toISOString());

      authApi.getCurrentUser().then((updatedUser) => {
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
      }).catch(() => {});

      // Classify in background — don't block the sync button / UI.
      emailApi.classifyInbox()
        .then(() => {
          queryClient.invalidateQueries({ queryKey: ['emails'] });
          queryClient.invalidateQueries({ queryKey: ['email-categories'] });
          queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
        })
        .catch(() => {});
    },
    onError: (error: unknown) => {
      console.error('Gmail sync error:', error);
    },
  });

  return {
    sync: syncMutation.mutate,
    isSyncing: syncMutation.isPending,
  };
}
