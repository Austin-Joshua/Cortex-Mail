import React from 'react';

interface BrandLogoProps {
  size?: number;
  showText?: boolean;
  textSize?: number;
  onClick?: () => void;
  className?: string;
}

export const BrandLogo: React.FC<BrandLogoProps> = ({
  size = 32,
  showText = true,
  textSize = 15,
  onClick,
  className = '',
}) => {
  const mark = (
    <>
      <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden="true" style={{ flexShrink: 0 }}>
        <defs>
          <linearGradient id="cortex-mark" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor="#1F2937" />
            <stop offset="100%" stopColor="#0B1220" />
          </linearGradient>
        </defs>
        <circle cx="16" cy="16" r="14" fill="url(#cortex-mark)" />
        <path
          d="M10 10.5 L16 21.5 L22 10.5"
          fill="none"
          stroke="#FFFFFF"
          strokeWidth="2.6"
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
              color: 'var(--v-ink)',
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
              background: 'linear-gradient(90deg, var(--v-ink) 0%, var(--v-ink-3) 70%, transparent 100%)',
              opacity: 0.85,
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
        aria-label="Cortex Mail — go to dashboard"
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
