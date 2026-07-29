import React from 'react';

interface PlaceholderProps {
  icon: React.ReactNode;
  tone?: string;
  headline: string;
  body: string;
  /** What this surface will do, as short capability lines. */
  points?: string[];
  action?: { label: string; onClick: () => void };
}

/**
 * Shared treatment for surfaces that are wired but not yet populated. Says
 * plainly what the page does instead of pretending to hold data.
 */
export const Placeholder: React.FC<PlaceholderProps> = ({
  icon,
  tone = 'var(--v-signal)',
  headline,
  body,
  points,
  action,
}) => (
  <div className="bento">
    <div className="tile v-c12 tile-feature v-rise" style={{ padding: 0 }}>
      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          textAlign: 'center',
          gap: 16,
          padding: '56px 24px',
          maxWidth: 520,
          margin: '0 auto',
        }}
      >
        <span
          className="glyph glyph-lg"
          style={
            {
              ['--glyph-fg']: tone,
              ['--glyph-wash']: `color-mix(in srgb, ${tone} 14%, transparent)`,
              width: 56,
              height: 56,
              borderRadius: 16,
            } as React.CSSProperties
          }
        >
          {icon}
        </span>

        <div>
          <h2 style={{ fontSize: 21, fontWeight: 800, letterSpacing: '-0.03em', margin: 0 }}>
            {headline}
          </h2>
          <p className="v-body" style={{ marginTop: 8 }}>{body}</p>
        </div>

        {points && points.length > 0 && (
          <ul
            style={{
              listStyle: 'none',
              margin: '4px 0 0',
              padding: 0,
              display: 'flex',
              flexWrap: 'wrap',
              gap: 8,
              justifyContent: 'center',
            }}
          >
            {points.map((p) => (
              <li key={p} className="chip" style={{ cursor: 'default' }}>
                <span className="dot" style={{ ['--dot']: tone } as React.CSSProperties} />
                {p}
              </li>
            ))}
          </ul>
        )}

        {action && (
          <button className="vbtn vbtn-signal" onClick={action.onClick} style={{ marginTop: 4 }}>
            {action.label}
          </button>
        )}
      </div>
    </div>
  </div>
);
