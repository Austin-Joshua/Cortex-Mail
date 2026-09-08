import type { User, UserRole } from '../types/User';
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  hasHydrated: boolean;
  setUser: (user: User) => void;
  setToken: (token: string) => void;
  setUserRole: (role: UserRole) => void;
  setLastSyncedAt: (date: string) => void;
  setHasHydrated: (value: boolean) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      token: null,
      isAuthenticated: false,
      hasHydrated: false,

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

      logout: () => {
        set({ user: null, token: null, isAuthenticated: false });
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
