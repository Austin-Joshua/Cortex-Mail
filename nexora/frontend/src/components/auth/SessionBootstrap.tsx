import { useEffect, useRef } from 'react';
import { authApi } from '../../api/authApi';
import { useAuthStore } from '../../store/authStore';
import type { AuthResponse } from '../../types/User';

/**
 * On boot, if a persisted JWT exists, refresh profile via /api/auth/me.
 * Clears the session on 401 so stale tokens don't leave a broken shell.
 */
export function SessionBootstrap() {
  const token = useAuthStore((s) => s.token);
  const setUser = useAuthStore((s) => s.setUser);
  const logout = useAuthStore((s) => s.logout);
  const started = useRef(false);

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
      .catch(() => {
        logout();
      });
  }, [token, setUser, logout]);

  return null;
}
