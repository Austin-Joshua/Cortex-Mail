import { useEffect, useRef } from 'react';
import { authApi } from '../../api/authApi';
import { useAuthStore } from '../../store/authStore';
import type { AuthResponse } from '../../types/User';
import axios from 'axios';

function applyAuthResponse(authResponse: AuthResponse) {
  const { setUser, setToken } = useAuthStore.getState();
  if (authResponse.token) {
    setToken(authResponse.token);
  }
  setUser({
    userId: authResponse.userId,
    email: authResponse.email,
    name: authResponse.name,
    profilePictureUrl: authResponse.profilePictureUrl,
    userRole: authResponse.userRole,
    onboardingComplete: authResponse.onboardingComplete,
    calendarSyncEnabled: authResponse.calendarSyncEnabled,
    lastSyncedAt: authResponse.lastSyncedAt,
    quietHoursStart: authResponse.quietHoursStart,
    quietHoursEnd: authResponse.quietHoursEnd,
    mutedCategories: authResponse.mutedCategories,
    digestEnabled: authResponse.digestEnabled,
    digestHour: authResponse.digestHour,
  });
}

/**
 * Cookie-first session restore: after persist hydrate, try /me (ACCESS_TOKEN cookie
 * or in-memory JWT), then /refresh. Only clears the session on 401/403.
 */
export function SessionBootstrap() {
  const setHasHydrated = useAuthStore((s) => s.setHasHydrated);
  const logout = useAuthStore((s) => s.logout);
  const started = useRef(false);

  useEffect(() => {
    const finish = () => setHasHydrated(true);
    const unsub = useAuthStore.persist.onFinishHydration(finish);
    if (useAuthStore.persist.hasHydrated()) {
      finish();
    }
    return unsub;
  }, [setHasHydrated]);

  useEffect(() => {
    if (started.current) return;
    started.current = true;

    const boot = async () => {
      // Wait for zustand persist so isAuthenticated/user are available.
      if (!useAuthStore.persist.hasHydrated()) {
        await new Promise<void>((resolve) => {
          const unsub = useAuthStore.persist.onFinishHydration(() => {
            unsub();
            resolve();
          });
        });
      }

      const state = useAuthStore.getState();
      try {
        const me = await authApi.getCurrentUser();
        applyAuthResponse(me);
        return;
      } catch (err: unknown) {
        const status = axios.isAxiosError(err) ? err.response?.status : undefined;
        if (status !== 401 && status !== 403) {
          // Transient network error — keep persisted session hints.
          return;
        }
      }

      try {
        const refreshed = await authApi.refreshSession();
        if (refreshed?.token || refreshed?.userId) {
          applyAuthResponse(refreshed);
          return;
        }
      } catch {
        /* fall through */
      }

      if (state.isAuthenticated || state.token) {
        logout();
      }
    };

    void boot();
  }, [logout]);

  return null;
}
