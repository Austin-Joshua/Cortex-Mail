import React, { useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { useSearchParams } from 'react-router-dom';
import { CheckCircle, AlertTriangle, X } from 'lucide-react';
import { CategoryTag } from '../components/common/CategoryTag';

const PREVIEW_EMAILS = [
  { sender: 'Dr. Sarah Chen', subject: 'Assignment 4 deadline extended to Friday 11:59 PM', category: 'ASSIGNMENT' },
  { sender: 'Devfolio', subject: 'Smart India Hackathon registrations close in 48h', category: 'HACKATHON' },
  { sender: 'TCS Nextstep', subject: 'Interview shortlist - Round 2 confirmed for you', category: 'PLACEMENT' },
  { sender: 'HOD Office', subject: 'Mandatory attendance policy change effective Monday', category: 'ANNOUNCEMENT' },
];

const TRUST_POINTS = [
  'Read-only Gmail access',
  'AES-256 token encryption',
  'Role-aware AI · Student-first',
];

export const LandingPage: React.FC = () => {
  const { handleGoogleLogin } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const authError = searchParams.get('auth_error');
  const [showErrorBanner, setShowErrorBanner] = useState(!!authError);

  const dismissError = () => {
    setShowErrorBanner(false);
    searchParams.delete('auth_error');
    setSearchParams(searchParams, { replace: true });
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        background: 'var(--bg)',
        color: 'var(--text-1)',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      <nav
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0 28px',
          height: 64,
          borderBottom: '1px solid var(--border)',
          flexShrink: 0,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 11 }}>
          <svg width="32" height="32" viewBox="0 0 32 32" aria-hidden="true">
            <defs>
              <linearGradient id="velocity-gold-landing" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stopColor="var(--v-signal-dim)" />
                <stop offset="100%" stopColor="var(--v-signal)" />
              </linearGradient>
            </defs>
            <path
              d="M 7.51 24.49 A 12 12 0 1 1 24.49 24.49"
              fill="none"
              stroke="url(#velocity-gold-landing)"
              strokeWidth="1.6"
              strokeLinecap="round"
              opacity="0.5"
            />
            <path
              d="M 10.4 11.2 L 16 20.6 L 21.6 11.2"
              fill="none"
              stroke="url(#velocity-gold-landing)"
              strokeWidth="3"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
            <circle cx="16" cy="26.2" r="2" fill="var(--v-ember)" />
          </svg>
          <span
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'flex-start',
              gap: 3,
              lineHeight: 1,
            }}
          >
            <span
              style={{
                fontSize: 14,
                fontWeight: 600,
                letterSpacing: '0.26em',
                textIndent: '0.26em',
                color: 'var(--text-1)',
              }}
            >
              VELOCITY
            </span>
            <span
              aria-hidden="true"
              style={{
                width: '100%',
                height: 1,
                background:
                  'linear-gradient(90deg, var(--v-signal) 0%, var(--v-ember) 65%, transparent 100%)',
                opacity: 0.65,
              }}
            />
          </span>
        </div>
        <div>
          <button
            onClick={handleGoogleLogin}
            className="btn-accent"
          >
            Sign in
          </button>
        </div>
      </nav>

      {showErrorBanner && (
        <div
          style={{
            margin: '16px 28px 0',
            background: 'var(--surface)',
            border: '1px solid var(--danger)',
            borderRadius: 8,
            padding: '12px 16px',
            display: 'flex',
            alignItems: 'center',
            gap: 12,
          }}
        >
          <AlertTriangle size={16} style={{ color: 'var(--danger)' }} />
          <div style={{ flex: 1, fontSize: 13, color: 'var(--danger)' }}>
            {authError === 'access_denied' ? 'Sign-In Cancelled' : 'Sign-In Failed. Please try again.'}
          </div>
          <button onClick={dismissError} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--danger)' }}>
            <X size={16} />
          </button>
        </div>
      )}

      <div
        style={{
          flex: 1,
          display: 'grid',
          gridTemplateColumns: '1fr 1fr',
          gap: 48,
          alignItems: 'center',
          maxWidth: 1100,
          margin: '0 auto',
          padding: '60px 24px',
          width: '100%',
        }}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
          <span style={{ fontSize: 12, fontWeight: 700, color: 'var(--accent)', textTransform: 'uppercase' }}>
            COMMUNICATION ACCELERATION PLATFORM
          </span>

          <h1
            style={{
              fontSize: 42,
              fontWeight: 700,
              color: 'var(--text-1)',
              lineHeight: 1.15,
              margin: 0,
              fontFamily: 'Google Sans, Roboto, sans-serif',
            }}
          >
            Accelerate your <span style={{ color: 'var(--accent)' }}>communication.</span>
          </h1>

          <p style={{ fontSize: 15, color: 'var(--text-2)', lineHeight: 1.6, margin: 0 }}>
            Velocity transforms your email management by accelerating communication, boosting productivity, and amplifying your professional impact.
          </p>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <button
              onClick={handleGoogleLogin}
              className="btn-accent"
              style={{ height: 48, padding: '0 24px', fontSize: 15, width: 'fit-content' }}
            >
              Connect Gmail Account
            </button>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              {TRUST_POINTS.map((point) => (
                <div key={point} style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 12, color: 'var(--text-2)' }}>
                  <CheckCircle size={14} style={{ color: 'var(--success)' }} />
                  <span>{point}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="surface-elevated" style={{ padding: 16, overflow: 'hidden' }}>
          <p className="section-label" style={{ marginBottom: 12 }}>INBOX PREVIEW</p>
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            {PREVIEW_EMAILS.map((email, i) => (
              <div
                key={i}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 12,
                  padding: '10px 12px',
                  borderBottom: '1px solid var(--border)',
                }}
              >
                <span style={{ fontSize: 13, fontWeight: 700, color: 'var(--text-1)', width: 110 }}>{email.sender}</span>
                <span style={{ flex: 1, fontSize: 13, color: 'var(--text-2)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{email.subject}</span>
                <CategoryTag category={email.category} />
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
