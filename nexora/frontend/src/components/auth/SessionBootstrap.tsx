import { useEffect, useRef } from 'react';
import { authApi } from '../../api/authApi';
import { useAuthStore } from '../../store/authStore';
import { applyAuthResponse } from '../../utils/applyAuthResponse';
import axios from 'axios';

/**
 * Cookie-first session restore: after persist hydrate, try /me (ACCESS_TOKEN cookie
 * or in-memory JWT), then /refresh. Gates the app via sessionReady until done.
 */
export function SessionBootstrap() {
  const setHasHydrated = useAuthStore((s) => s.setHasHydrated);
  const setSessionReady = useAuthStore((s) => s.setSessionReady);
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
        setSessionReady(true);
        return;
      } catch (err: unknown) {
        const status = axios.isAxiosError(err) ? err.response?.status : undefined;
        if (status !== 401 && status !== 403) {
          // Transient network error — keep persist hints; allow UI through.
          setSessionReady(true);
          return;
        }
      }

      try {
        const refreshed = await authApi.refreshSession();
        if (refreshed?.token || refreshed?.userId) {
          applyAuthResponse(refreshed);
          setSessionReady(true);
          return;
        }
      } catch {
        /* fall through */
      }

      if (state.isAuthenticated || state.token) {
        logout();
      } else {
        setSessionReady(true);
      }
    };

    void boot();
  }, [logout, setSessionReady]);

  return null;
}
