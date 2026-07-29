import React, { useEffect, useState } from 'react';
import { useInView } from '../../hooks/useReveal';

interface OdometerProps {
  to: number;
  /** Milliseconds for the full run. */
  duration?: number;
  decimals?: number;
  prefix?: string;
  suffix?: string;
  className?: string;
  style?: React.CSSProperties;
}

const easeOut = (t: number) => 1 - Math.pow(1 - t, 3);

/**
 * Counts up to `to` the first time it scrolls into view. Digits are tabular
 * so the figure does not jitter as it climbs.
 */
export const Odometer: React.FC<OdometerProps> = ({
  to,
  duration = 1400,
  decimals = 0,
  prefix = '',
  suffix = '',
  className,
  style,
}) => {
  const { ref, seen } = useInView<HTMLSpanElement>();
  const [value, setValue] = useState(0);

  useEffect(() => {
    if (!seen) return;

    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (reduced) {
      setValue(to);
      return;
    }

    let frame = 0;
    const start = performance.now();

    const step = (now: number) => {
      const t = Math.min(1, (now - start) / duration);
      setValue(to * easeOut(t));
      if (t < 1) frame = requestAnimationFrame(step);
    };

    frame = requestAnimationFrame(step);
    return () => cancelAnimationFrame(frame);
  }, [seen, to, duration]);

  return (
    <span
      ref={ref}
      className={className}
      style={{ fontVariantNumeric: 'tabular-nums', ...style }}
    >
      {prefix}
      {value.toFixed(decimals)}
      {suffix}
    </span>
  );
};
