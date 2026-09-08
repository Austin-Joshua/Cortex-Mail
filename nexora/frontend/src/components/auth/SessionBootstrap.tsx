import { useEffect, useRef } from 'react';
import { authApi } from '../../api/authApi';
import { useAuthStore } from '../../store/authStore';
import type { AuthResponse } from '../../types/User';
import axios from 'axios';

/**
 * On boot, if a persisted JWT exists, refresh profile via /api/auth/me.
 * Only clears the session on 401/403 so transient network errors do not force re-login.
 */
export function SessionBootstrap() {
  const token = useAuthStore((s) => s.token);
  const setUser = useAuthStore((s) => s.setUser);
  const logout = useAuthStore((s) => s.logout);
  const setHasHydrated = useAuthStore((s) => s.setHasHydrated);
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
    if (!token || started.current) return;
    started.current = true;
    void authApi.getCurrentUser()
      .then((authResponse: AuthResponse) => {
        setUser({
          userId: authResponse.userId,
          email: authResponse.email,
          name: authResponse.name,
          profilePictureUrl: authResponse.profilePictureUrl,
          userRole: authResponse.userRole,
          onboardingComplete: authResponse.onboardingComplete,
          calendarSyncEnabled: authResponse.calendarSyncEnabled,
          lastSyncedAt: authResponse.lastSyncedAt,
        });
      })
      .catch((err: unknown) => {
        const status = axios.isAxiosError(err) ? err.response?.status : undefined;
        if (status === 401 || status === 403) {
          logout();
        }
      });
  }, [token, setUser, logout]);

  return null;
}
