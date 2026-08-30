import React from 'react';

interface GaugeProps {
  /** 0–100 */
  value: number;
  size?: number;
  label?: string;
  /** Stroke color; defaults to the signal token. */
  tone?: string;
  /** When true, show placeholder instead of a numeric score. */
  pending?: boolean;
}

/**
 * A 270° instrument arc. The track is drawn once, the value arc sweeps in on
 * mount via stroke-dashoffset. pathLength is normalised to 100 so the offset
 * maths is just `100 - value`.
 */
export const Gauge: React.FC<GaugeProps> = ({ value, size = 152, label, tone, pending }) => {
  const v = pending ? 0 : Math.max(0, Math.min(100, value));
  // 270° arc on a r=52 circle centred at (70,70), opening at the bottom.
  const d = 'M 33.23 106.77 A 52 52 0 1 1 106.77 106.77';

  return (
    <div
      className={pending ? 'gauge-pending' : undefined}
      style={{
        position: 'relative',
        width: size,
        height: size,
        flexShrink: 0,
      }}
    >
      <svg viewBox="0 0 140 140" width={size} height={size} aria-hidden="true">
        <path
          d={d}
          fill="none"
          stroke="var(--v-hairline)"
          strokeWidth="9"
          strokeLinecap="round"
        />
        <path
          className="gauge-arc"
          d={d}
          fill="none"
          stroke={pending ? 'var(--v-hairline)' : (tone ?? 'var(--v-signal)')}
          strokeWidth="9"
          strokeLinecap="round"
          pathLength={100}
          strokeDasharray="100"
          strokeDashoffset={pending ? 100 : 100 - v}
          style={{ ['--dash-len' as string]: '100' } as React.CSSProperties}
        />
      </svg>

      <div
        style={{
          position: 'absolute',
          inset: 0,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 4,
          paddingBottom: size * 0.06,
          pointerEvents: 'none',
        }}
      >
        <span className="v-readout v-readout-xl" style={{ fontSize: pending ? size * 0.22 : size * 0.3, color: pending ? 'var(--color-text-muted)' : (tone ?? 'var(--color-cortex)') }}>
          {pending ? '—' : v}
        </span>
        {label && <span className="v-label">{label}</span>}
      </div>
    </div>
  );
};
