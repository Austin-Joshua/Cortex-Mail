import { useEffect } from 'react';
import { useThemeStore } from '../../store/themeStore';
import { applyThemeMode, readStoredTheme, THEME_STORAGE_KEY } from '../../utils/theme';

/** Keeps resolved theme in sync when OS preference or another tab changes theme. */
export function ThemeSync() {
  const mode = useThemeStore((s) => s.mode);
  const setMode = useThemeStore((s) => s.setMode);
  const syncFromSystem = useThemeStore((s) => s.syncFromSystem);

  useEffect(() => {
    const mq = window.matchMedia('(prefers-color-scheme: light)');
    const onChange = () => syncFromSystem();
    mq.addEventListener('change', onChange);
    return () => mq.removeEventListener('change', onChange);
  }, [syncFromSystem]);

  useEffect(() => {
    const onStorage = (e: StorageEvent) => {
      if (e.key !== THEME_STORAGE_KEY || e.newValue == null) return;
      const next = readStoredTheme();
      setMode(next);
    };
    window.addEventListener('storage', onStorage);
    return () => window.removeEventListener('storage', onStorage);
  }, [setMode]);

  useEffect(() => {
    applyThemeMode(mode);
  }, [mode]);

  return null;
}
