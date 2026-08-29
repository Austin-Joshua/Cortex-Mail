import React, { useId } from 'react';

interface BrandLogoProps {
  size?: number;
  showText?: boolean;
  textSize?: number;
  onClick?: () => void;
  className?: string;
  /** Light text for navy / dark surfaces */
  inverted?: boolean;
}

export const BrandLogo: React.FC<BrandLogoProps> = ({
  size = 32,
  showText = true,
  textSize = 15,
  onClick,
  className = '',
  inverted = false,
}) => {
  const gid = useId().replace(/:/g, '');
  const ink = inverted ? '#FFFFFF' : 'var(--v-ink)';
  const bar = inverted
    ? 'linear-gradient(90deg, #FFFFFF 0%, rgba(255,255,255,0.35) 70%, transparent 100%)'
    : 'linear-gradient(90deg, var(--v-navy) 0%, var(--v-navy-mid) 55%, transparent 100%)';

  const mark = (
    <>
      <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden="true" style={{ flexShrink: 0 }}>
        <defs>
          <linearGradient id={`cm-mark-${gid}`} x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor="#2563EB" />
            <stop offset="55%" stopColor="#1E3A8A" />
            <stop offset="100%" stopColor="#172554" />
          </linearGradient>
        </defs>
        <rect x="1" y="1" width="30" height="30" rx="9" fill={`url(#cm-mark-${gid})`} />
        {/* Envelope fold — mail mark */}
        <path
          d="M7.5 11.2h17v10.6c0 .7-.5 1.2-1.2 1.2H8.7c-.7 0-1.2-.5-1.2-1.2V11.2z"
          fill="none"
          stroke="#FFFFFF"
          strokeWidth="1.7"
          strokeLinejoin="round"
        />
        <path
          d="M8 11.5 L16 17.2 L24 11.5"
          fill="none"
          stroke="#FFFFFF"
          strokeWidth="1.7"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
      {showText && (
        <span
          style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'flex-start',
            gap: 2,
            lineHeight: 1.1,
          }}
        >
          <span
            style={{
              fontSize: textSize,
              fontWeight: 800,
              letterSpacing: '-0.02em',
              color: ink,
            }}
          >
            Cortex Mail
          </span>
          <span
            aria-hidden="true"
            style={{
              width: '100%',
              height: 2,
              borderRadius: 999,
              background: bar,
              opacity: 0.9,
            }}
          />
        </span>
      )}
    </>
  );

  if (onClick) {
    return (
      <button
        type="button"
        onClick={onClick}
        aria-label="Cortex Mail — home"
        className={className}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 10,
          background: 'none',
          border: 'none',
          cursor: 'pointer',
          padding: '4px 6px',
        }}
      >
        {mark}
      </button>
    );
  }

  return (
    <span className={className} style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
      {mark}
    </span>
  );
};
