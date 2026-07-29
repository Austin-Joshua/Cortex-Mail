import React from 'react';

interface TileProps {
  /** Column span on the 12-col desktop grid. Collapses automatically below. */
  span?: 3 | 4 | 5 | 6 | 7 | 8 | 12;
  /** Row span. */
  rows?: 1 | 2 | 3;
  /** Renders the accent-washed hero treatment. */
  feature?: boolean;
  /** Draws a 3px state rule across the tile's top edge. */
  rule?: string;
  onClick?: () => void;
  className?: string;
  style?: React.CSSProperties;
  /** Stagger index — each tile rises slightly after the previous one. */
  index?: number;
  children: React.ReactNode;
}

export const Tile: React.FC<TileProps> = ({
  span = 3,
  rows = 1,
  feature,
  rule,
  onClick,
  className = '',
  style,
  index = 0,
  children,
}) => {
  const classes = [
    'tile',
    `v-c${span}`,
    rows > 1 ? `v-r${rows}` : '',
    feature ? 'tile-feature' : '',
    rule ? 'tile-rule' : '',
    onClick ? 'tile-link' : '',
    'v-rise',
    className,
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <div
      className={classes}
      onClick={onClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
      onKeyDown={
        onClick
          ? (e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                onClick();
              }
            }
          : undefined
      }
      style={{
        ...(rule ? ({ ['--rule' as string]: rule } as React.CSSProperties) : {}),
        animationDelay: `${Math.min(index, 10) * 45}ms`,
        ...style,
      }}
    >
      {children}
    </div>
  );
};

interface TileHeadProps {
  label: string;
  icon?: React.ReactNode;
  tone?: string;
  right?: React.ReactNode;
}

export const TileHead: React.FC<TileHeadProps> = ({ label, icon, tone, right }) => (
  <div className="tile-head">
    <div style={{ display: 'flex', alignItems: 'center', gap: 10, minWidth: 0 }}>
      {icon && (
        <span
          className="glyph"
          style={
            tone
              ? ({
                  ['--glyph-fg' as string]: tone,
                  ['--glyph-wash' as string]: `color-mix(in srgb, ${tone} 14%, transparent)`,
                } as React.CSSProperties)
              : undefined
          }
        >
          {icon}
        </span>
      )}
      {/* Wraps rather than clips — half-width tiles on mobile are too narrow
          for a single line, and a truncated label tells you nothing. */}
      <span className="v-label" style={{ lineHeight: 1.35, minWidth: 0 }}>{label}</span>
    </div>
    {right}
  </div>
);
