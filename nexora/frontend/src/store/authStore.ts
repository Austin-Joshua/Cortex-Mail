import type { User, UserRole } from '../types/User';
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  /** Persist rehydrate finished (localStorage). */
  hasHydrated: boolean;
  /** Cookie/session bootstrap finished (/me or /refresh). */
  sessionReady: boolean;
  setUser: (user: User) => void;
  setToken: (token: string) => void;
  setUserRole: (role: UserRole) => void;
  setLastSyncedAt: (date: string) => void;
  setHasHydrated: (value: boolean) => void;
  setSessionReady: (value: boolean) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      token: null,
      isAuthenticated: false,
      hasHydrated: false,
      sessionReady: false,

      setUser: (user) => set({ user, isAuthenticated: true }),

      setToken: (token) => set({ token }),

      setUserRole: (role) =>
        set((state) => ({
          user: state.user ? { ...state.user, userRole: role } : null,
        })),

      setLastSyncedAt: (date) =>
        set((state) => ({
          user: state.user ? { ...state.user, lastSyncedAt: date } : null,
        })),

      setHasHydrated: (value) => set({ hasHydrated: value }),

      setSessionReady: (value) => set({ sessionReady: value }),

      logout: () => {
        set({
          user: null,
          token: null,
          isAuthenticated: false,
          sessionReady: true,
        });
        try {
          localStorage.removeItem('cortex_auth');
        } catch {
          /* private mode / blocked storage */
        }
      },
    }),
    {
      name: 'cortex_auth',
      partialize: (state) => ({
        user: state.user,
        isAuthenticated: state.isAuthenticated,
      }),
      onRehydrateStorage: () => (state) => {
        state?.setHasHydrated(true);
      },
    }
  )
);
