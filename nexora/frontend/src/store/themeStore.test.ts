import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  THEME_STORAGE_KEY,
  applyThemeToDocument,
  resolveInitialTheme,
} from './themeStore';

function mockStorage() {
  const map = new Map<string, string>();
  return {
    getItem: (key: string) => map.get(key) ?? null,
    setItem: (key: string, value: string) => {
      map.set(key, value);
    },
    removeItem: (key: string) => {
      map.delete(key);
    },
    clear: () => {
      map.clear();
    },
  };
}

describe('themeStore helpers', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('reads a saved theme from localStorage', () => {
    const storage = mockStorage();
    storage.setItem(THEME_STORAGE_KEY, 'light');
    vi.stubGlobal('localStorage', storage);
    vi.stubGlobal('window', { matchMedia: () => ({ matches: false }) });
    expect(resolveInitialTheme()).toBe('light');
  });

  it('falls back to system preference when nothing is saved', () => {
    vi.stubGlobal('localStorage', mockStorage());
    vi.stubGlobal('window', {
      matchMedia: (query: string) => ({ matches: query.includes('prefers-color-scheme: light') }),
    });
    expect(resolveInitialTheme()).toBe('light');
  });

  it('applies data-theme and color-scheme to the document', () => {
    const attrs: Record<string, string> = {};
    const style: Record<string, string> = {};
    const metaAttrs: Record<string, string> = {};
    const themeColor = {
      setAttribute: (k: string, v: string) => {
        metaAttrs[k] = v;
      },
      getAttribute: (k: string) => metaAttrs[k] ?? null,
    };

    vi.stubGlobal('document', {
      documentElement: {
        setAttribute: (k: string, v: string) => {
          attrs[k] = v;
        },
        getAttribute: (k: string) => attrs[k] ?? null,
        style,
      },
      querySelector: (sel: string) => (sel.includes('theme-color') ? themeColor : null),
    });

    applyThemeToDocument('light');
    expect(attrs['data-theme']).toBe('light');
    expect(style.colorScheme).toBe('light');
    expect(metaAttrs.content).toBe('#F4F6FA');

    applyThemeToDocument('dark');
    expect(attrs['data-theme']).toBe('dark');
    expect(metaAttrs.content).toBe('#0B0F19');
  });
});
