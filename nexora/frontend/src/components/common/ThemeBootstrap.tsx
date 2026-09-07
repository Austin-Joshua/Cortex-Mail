import { useEffect } from 'react';
import { applyThemeToDocument, useThemeStore } from '../../store/themeStore';

/** Keeps document theme attributes in sync with the store (and FOUC script). */
export function ThemeBootstrap() {
  const theme = useThemeStore((s) => s.theme);

  useEffect(() => {
    applyThemeToDocument(theme);
  }, [theme]);

  return null;
}
