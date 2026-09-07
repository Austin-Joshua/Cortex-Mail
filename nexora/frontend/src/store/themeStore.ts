import { create } from 'zustand';
import {
  applyThemeMode,
  getSystemTheme,
  readStoredTheme,
  resolveTheme,
  type ResolvedTheme,
  type ThemeMode,
} from '../utils/theme';

interface ThemeStore {
  mode: ThemeMode;
  resolved: ResolvedTheme;
  setMode: (mode: ThemeMode) => void;
  toggleLightDark: () => void;
  syncFromSystem: () => void;
}

function bootTheme(): Pick<ThemeStore, 'mode' | 'resolved'> {
  const mode = typeof window !== 'undefined' ? readStoredTheme() : 'system';
  const resolved =
    typeof window !== 'undefined' ? applyThemeMode(mode) : resolveTheme(mode);
  return { mode, resolved };
}

export const useThemeStore = create<ThemeStore>((set, get) => ({
  ...bootTheme(),

  setMode: (mode) => {
    const resolved = applyThemeMode(mode);
    set({ mode, resolved });
  },

  toggleLightDark: () => {
    const next: ThemeMode = get().resolved === 'light' ? 'dark' : 'light';
    const resolved = applyThemeMode(next);
    set({ mode: next, resolved });
  },

  syncFromSystem: () => {
    if (get().mode !== 'system') return;
    const resolved = getSystemTheme();
    applyThemeMode('system');
    set({ resolved });
  },
}));
