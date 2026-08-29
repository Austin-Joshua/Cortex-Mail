import { useState, useEffect } from 'react';

export interface ViewportState {
  width: number;
  height: number;
  isMobile: boolean;
  isTablet: boolean;
  isDesktop: boolean;
}

function computeViewport(width: number, height: number): ViewportState {
  return {
    width,
    height,
    isMobile: width < 768,
    isTablet: width >= 768 && width < 1280,
    isDesktop: width >= 1280,
  };
}

export function useViewport(): ViewportState {
  const [viewport, setViewport] = useState<ViewportState>(() => {
    const width = typeof window !== 'undefined' ? window.innerWidth : 1280;
    const height = typeof window !== 'undefined' ? window.innerHeight : 800;
    return computeViewport(width, height);
  });

  useEffect(() => {
    let frame = 0;
    let timeout: ReturnType<typeof setTimeout> | undefined;

    const commit = () => {
      frame = 0;
      setViewport(computeViewport(window.innerWidth, window.innerHeight));
    };

    const handleResize = () => {
      if (frame) cancelAnimationFrame(frame);
      clearTimeout(timeout);
      timeout = setTimeout(() => {
        frame = requestAnimationFrame(commit);
      }, 100);
    };

    window.addEventListener('resize', handleResize, { passive: true });
    return () => {
      window.removeEventListener('resize', handleResize);
      if (frame) cancelAnimationFrame(frame);
      clearTimeout(timeout);
    };
  }, []);

  return viewport;
}
