import React, { useEffect, useRef, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import {
  AlertTriangle, X, ArrowRight, ChevronDown, Lock, EyeOff, KeyRound,
  Brain, Timer, Flame, CalendarClock,
} from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import { useAuthStore } from '../store/authStore';
import { useReveal, useScrollProgress } from '../hooks/useReveal';
import { Odometer } from '../components/landing/Odometer';
import { Gauge } from '../components/bento/Gauge';
import { BrandLogo } from '../components/common/BrandLogo';
import '../styles/landing.css';

/* ------------------------------------------------------------------ data */

const LEDGER = [
  { label: 'Unread backlog',   debit: 27, tone: 'var(--v-red)',    note: '23 sitting unopened' },
  { label: 'Open actions',     debit: 12, tone: 'var(--v-orange)', note: '4 asks with no reply' },
  { label: 'Overdue deadlines', debit: 6, tone: 'var(--v-red)',   note: '1 already past' },
];

const ZONES = [
  { name: 'Deep Focus',    hours: '9 — 12', note: 'Notifications muted',  tone: 'var(--color-cortex)' },
  { name: 'Collaboration', hours: '12 — 3', note: 'Notifications live',   tone: 'var(--v-orange)' },
  { name: 'Rapid Fire',    hours: '3 — 5',  note: 'Batch the quick ones', tone: 'var(--v-red)' },
  { name: 'Reflection',    hours: '5 — 7',  note: 'Notifications muted',  tone: 'var(--color-cortex-light)' },
];

const CAPABILITIES = [
  {
    icon: Flame,
    tone: 'var(--v-red)',
    title: 'It ranks before you read',
    body: 'Every message lands in one of three bands — act now, today, when clear. The top of the list is always the next thing to do.',
  },
  {
    icon: Timer,
    tone: 'var(--v-orange)',
    title: 'It finds the dates you missed',
    body: 'Deadlines buried in the fourth paragraph get pulled out, counted down, and pushed to your calendar before they turn red.',
  },
  {
    icon: CalendarClock,
    tone: 'var(--v-green)',
    title: 'It defends your focus',
    body: 'Flow zones split the day into bands. During Deep Focus nothing interrupts you; the backlog waits until you are ready for it.',
  },
  {
    icon: Brain,
    tone: 'var(--color-cortex)',
    title: 'It answers in your own words',
    body: 'Ask what recruiters sent last week, or which deadlines land before Friday. Answers link straight back to the mail.',
  },
];

const TRUST = [
  { icon: EyeOff,   title: 'You stay in control', body: 'Cortex Mail syncs and ranks your mail. Mailbox changes (star, archive, trash) happen only when you click them.' },
  { icon: KeyRound, title: 'Encrypted at rest',   body: 'Google tokens are sealed with AES-256 and never leave your workspace.' },
  { icon: Lock,     title: 'Not training data',   body: 'Your messages are yours. They are never used to train any model.' },
];

/* ------------------------------------------------------------ components */

/** Wraps children in a scroll-revealed block. */
const Reveal: React.FC<{
  i?: number;
  variant?: 'up' | 'left' | 'right' | 'scale';
  className?: string;
  style?: React.CSSProperties;
  children: React.ReactNode;
}> = ({ i = 0, variant = 'up', className = '', style, children }) => {
  const ref = useReveal<HTMLDivElement>();
  const variantClass = variant === 'up' ? '' : `reveal-${variant}`;
  return (
    <div
      ref={ref}
      className={`reveal ${variantClass} ${className}`.trim()}
      style={{ ['--i' as string]: i, ...style } as React.CSSProperties}
    >
      {children}
    </div>
  );
};

/* ------------------------------------------------------------------ page */

export const LandingPage: React.FC = () => {
  const navigate = useNavigate();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const { handleGoogleLogin } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const authError = searchParams.get('auth_error');
  const errorDescription = searchParams.get('error_description') ?? '';
  const [showError, setShowError] = useState(!!authError);
  const [stuck, setStuck] = useState(false);
  const progress = useScrollProgress();
  const heroRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (isAuthenticated && !authError) {
      navigate('/dashboard', { replace: true });
    }
  }, [isAuthenticated, authError, navigate]);

  useEffect(() => {
    const onScroll = () => setStuck(window.scrollY > 12);
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  const dismissError = () => {
    setShowError(false);
    searchParams.delete('auth_error');
    searchParams.delete('error_description');
    setSearchParams(searchParams, { replace: true });
  };

  const authErrorMessage = (() => {
    switch (authError) {
      case 'access_denied':
        return 'Sign-in was cancelled. Nothing was connected.';
      case 'oauth_not_configured':
      case 'invalid_client':
        return 'Google OAuth is not configured. Add your Client ID and Secret, then try Connect Gmail again.';
      case 'missing_code':
        return 'Sign-in did not return an authorization code. Try Connect Gmail again.';
      case 'oauth_failed': {
        const detail = errorDescription.toLowerCase();
        if (
          detail.includes('jdbc') ||
          detail.includes('hikari') ||
          detail.includes('connection is not available') ||
          detail.includes('database')
        ) {
          return 'Sign-in reached Google, but the server could not reach the database. Stop any extra backend instances, wait a few seconds, then try Connect Gmail again.';
        }
        return 'Google sign-in failed. Check your Client ID, Secret, and redirect URI.';
      }
      default:
        return 'Sign-in did not complete. Please try Connect Gmail again.';
    }
  })();

  const scrollOn = () =>
    window.scrollTo({
      top: (heroRef.current?.offsetHeight ?? 600) + 40,
      behavior: 'smooth',
    });

  return (
    <div className="lp">
      <div className="lp-rail" style={{ ['--p' as string]: progress } as React.CSSProperties} />

      <nav className="lp-nav" data-stuck={stuck ? '1' : '0'}>
        <BrandLogo
          size={32}
          textSize={14}
          onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
        />
        <button
          className="lp-btn lp-btn-primary"
          onClick={handleGoogleLogin}
          style={{ height: 42, padding: '0 20px', fontSize: 13.5 }}
        >
          Sign in
        </button>
      </nav>

      {showError && (
        <div
          role="alert"
          style={{
            margin: '16px auto 0',
            maxWidth: 1180,
            display: 'flex',
            alignItems: 'center',
            gap: 12,
            padding: '13px 16px',
            background: 'var(--v-critical-wash)',
            border: '1px solid var(--v-critical)',
            borderRadius: 12,
            color: 'var(--v-critical)',
            fontSize: 13.5,
            fontWeight: 600,
          }}
        >
          <AlertTriangle size={16} style={{ flexShrink: 0 }} />
          <span style={{ flex: 1 }}>{authErrorMessage}</span>
          <button
            onClick={dismissError}
            aria-label="Dismiss"
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'inherit', display: 'flex' }}
          >
            <X size={16} />
          </button>
        </div>
      )}

      {/* ---------------------------------------------------------- HERO */}
      <header className="lp-section lp-hero" ref={heroRef}>
        <div className="lp-atmos" aria-hidden="true" />

        <div style={{ display: 'flex', flexDirection: 'column', gap: 22 }}>
          <Reveal i={0}>
            <div className="lp-brand-hero">
              <BrandLogo size={48} showText={false} />
              <h1 className="lp-brand-name">Cortex Mail</h1>
            </div>
          </Reveal>

          <Reveal i={1}>
            <span className="lp-eyebrow">AI email intelligence</span>
          </Reveal>

          <Reveal i={2}>
            <p className="lp-display" style={{ maxWidth: '12ch', fontSize: 'clamp(32px, 5.8vw, 64px)' }}>
              Your inbox, understood.
            </p>
          </Reveal>

          <Reveal i={3}>
            <p className="lp-lead">
              An instrument on your Gmail — navy-clear priorities, deadlines pulled
              into view, and hours back from deciding what to read.
            </p>
          </Reveal>

          <Reveal i={4}>
            <div className="lp-cta-row">
              <button className="lp-btn lp-btn-primary" onClick={handleGoogleLogin}>
                Connect Gmail <ArrowRight size={17} />
              </button>
              <button className="lp-btn lp-btn-ghost" onClick={scrollOn}>
                See how it reads
              </button>
            </div>
          </Reveal>

          <Reveal i={5}>
            <p style={{ fontSize: 12.5, color: 'var(--v-ink-3)', margin: 0 }}>
              Encrypted tokens · Disconnect anytime · You control every mailbox action
            </p>
          </Reveal>
        </div>

        <Reveal variant="scale" i={2} className="lp-hero-art">
          <div className="lp-panel" style={{ display: 'flex', flexDirection: 'column', gap: 22 }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}>
              <span className="v-label" style={{ color: 'var(--color-cortex-light)' }}>Cortex Score</span>
              <span
                style={{
                  fontSize: 11, fontWeight: 700, padding: '4px 10px', borderRadius: 999,
                  background: 'var(--color-cortex-soft)', color: 'var(--color-cortex-light)',
                }}
              >
                Backlog building
              </span>
            </div>

            <div style={{ display: 'flex', justifyContent: 'center', paddingBlock: 4 }}>
              <Gauge value={55} tone="var(--color-cortex)" label="of 100" size={186} />
            </div>

            <div style={{ display: 'flex', gap: 10 }}>
              {[
                { k: 'Backlog', v: 23, tone: 'var(--v-red)' },
                { k: 'Actions', v: 4, tone: 'var(--v-orange)' },
                { k: 'Overdue', v: 1, tone: 'var(--v-red)' },
              ].map((s) => (
                <div
                  key={s.k}
                  style={{
                    flex: 1, minWidth: 0, padding: '12px 14px', borderRadius: 13,
                    background: 'var(--color-cortex-soft)', border: '1px solid var(--color-cortex-border)',
                  }}
                >
                  <div style={{ fontSize: 22, fontWeight: 800, letterSpacing: '-0.03em', color: s.tone, fontVariantNumeric: 'tabular-nums' }}>
                    {s.v}
                  </div>
                  <div className="v-label" style={{ marginTop: 6 }}>{s.k}</div>
                </div>
              ))}
            </div>
          </div>
        </Reveal>
      </header>

      <div style={{ display: 'flex', justifyContent: 'center', marginTop: -40, marginBottom: 24 }}>
        <button
          onClick={scrollOn}
          aria-label="Scroll to content"
          className="lp-cue"
          style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--v-ink-3)', display: 'flex' }}
        >
          <ChevronDown size={26} />
        </button>
      </div>

      {/* --------------------------------------------------------- COST */}
      <section className="lp-section" style={{ paddingBlock: 'clamp(48px, 8vh, 96px)' }}>
        <Reveal>
          <div className="lp-stats">
            {[
              { n: 28, suffix: '%', label: 'of the working week goes to mail', d: 0 },
              { n: 2.6, suffix: 'h', label: 'lost daily just deciding what matters', d: 1, dec: 1 },
              { n: 1, suffix: ' in 5', label: 'deadlines are found too late', d: 2 },
            ].map((s) => (
              <div key={s.label} style={{ ['--i' as string]: s.d } as React.CSSProperties}>
                <Odometer
                  to={s.n}
                  decimals={s.dec ?? 0}
                  suffix={s.suffix}
                  style={{
                    display: 'block',
                    fontSize: 'clamp(40px, 6vw, 68px)',
                    fontWeight: 800,
                    letterSpacing: '-0.045em',
                    lineHeight: 1,
                    color: 'var(--color-cortex-light)',
                  }}
                />
                <p style={{ margin: '12px 0 0', fontSize: 13.5, color: 'var(--v-ink-2)', maxWidth: '26ch' }}>
                  {s.label}
                </p>
              </div>
            ))}
          </div>
        </Reveal>
      </section>

      {/* -------------------------------------------------------- LEDGER */}
      <section className="lp-section lp-band">
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'minmax(0, 0.95fr) minmax(0, 1.05fr)',
            gap: 'clamp(28px, 5vw, 64px)',
            alignItems: 'center',
          }}
          className="lp-two"
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
            <Reveal variant="left" i={0}>
              <span className="lp-eyebrow">The instrument</span>
            </Reveal>
            <Reveal variant="left" i={1}>
              <h2 className="lp-h2">
                One number,<br />and it is <span className="lp-gold">honest</span>.
              </h2>
            </Reveal>
            <Reveal variant="left" i={2}>
              <p className="lp-lead">
                Your score starts at a hundred and is debited by the three things that
                actually slow you down. Nothing is weighted by a vanity metric, and
                nothing is invented — clear the backlog and the number climbs on its own.
              </p>
            </Reveal>
          </div>

          <Reveal variant="right" className="lp-panel">
            <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginBottom: 18 }}>
              <span className="v-label">Score ledger</span>
              <span style={{ fontSize: 13, color: 'var(--v-ink-3)', fontVariantNumeric: 'tabular-nums' }}>
                starts at 100
              </span>
            </div>

            <div className="lp-ledger">
              {LEDGER.map((row) => (
                <div key={row.label} className="lp-ledger-row">
                  <div style={{ minWidth: 0, flex: '0 0 42%' }}>
                    <div style={{ fontSize: 13.5, fontWeight: 700 }}>{row.label}</div>
                    <div style={{ fontSize: 11.5, color: 'var(--v-ink-3)', marginTop: 3 }}>{row.note}</div>
                  </div>
                  <div
                    className="lp-ledger-bar"
                    style={{ ['--w' as string]: `${row.debit * 2.4}%`, ['--bar' as string]: row.tone } as React.CSSProperties}
                  >
                    <i />
                  </div>
                  <div
                    style={{
                      width: 46, textAlign: 'right', fontSize: 14, fontWeight: 800,
                      color: row.tone, fontVariantNumeric: 'tabular-nums',
                    }}
                  >
                    −{row.debit}
                  </div>
                </div>
              ))}
            </div>

            <div
              style={{
                marginTop: 18, paddingTop: 18, borderTop: '1px solid var(--v-hairline-2)',
                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
              }}
            >
              <span className="v-label">Today</span>
              <span style={{ fontSize: 34, fontWeight: 800, letterSpacing: '-0.04em', fontVariantNumeric: 'tabular-nums' }}>
                <Odometer to={55} /> <span style={{ fontSize: 15, color: 'var(--v-ink-3)', fontWeight: 600 }}>/ 100</span>
              </span>
            </div>
          </Reveal>
        </div>
      </section>

      {/* --------------------------------------------------------- ZONES */}
      <section className="lp-band-strong">
        <div className="lp-section">
        <div style={{ display: 'flex', flexDirection: 'column', gap: 26 }}>
          <Reveal i={0}>
            <span className="lp-eyebrow">Flow zones</span>
          </Reveal>
          <Reveal i={1}>
            <h2 className="lp-h2" style={{ maxWidth: '18ch' }}>
              The day is not one <span className="lp-ember">long</span> inbox.
            </h2>
          </Reveal>
          <Reveal i={2}>
            <p className="lp-lead">
              Cortex Mail splits your working hours into bands and holds the noise back
              during the ones that matter. Deep work stays deep; the quick replies get
              batched into the window built for them.
            </p>
          </Reveal>

          <Reveal i={3} className="v-xscroll" style={{ paddingBottom: 4 }}>
            <div className="lp-strip">
              {ZONES.map((z, i) => (
                <div key={z.name} className="lp-zone" style={{ ['--i' as string]: i } as React.CSSProperties}>
                  <span
                    style={{
                      display: 'block', width: 30, height: 3, borderRadius: 999,
                      background: z.tone, marginBottom: 14,
                    }}
                  />
                  <div style={{ fontSize: 14.5, fontWeight: 700 }}>{z.name}</div>
                  <div style={{ fontSize: 12.5, color: 'var(--v-ink-2)', marginTop: 5, fontVariantNumeric: 'tabular-nums' }}>
                    {z.hours}
                  </div>
                  <div className="v-label" style={{ marginTop: 12 }}>{z.note}</div>
                </div>
              ))}
            </div>
          </Reveal>
        </div>
        </div>
      </section>

      {/* -------------------------------------------------- CAPABILITIES */}
      <section className="lp-section">
        <div style={{ display: 'flex', flexDirection: 'column', gap: 30 }}>
          <Reveal i={0}>
            <span className="lp-eyebrow">What it does</span>
          </Reveal>
          <Reveal i={1}>
            <h2 className="lp-h2" style={{ maxWidth: '20ch' }}>
              Four things, done <span className="lp-gold">properly</span>.
            </h2>
          </Reveal>

          <div className="lp-grid">
            {CAPABILITIES.map((c, i) => {
              const Icon = c.icon;
              return (
                <Reveal key={c.title} i={i} variant="scale">
                  <article className="lp-card" style={{ height: '100%' }}>
                    <span
                      style={{
                        display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                        width: 42, height: 42, borderRadius: 13, marginBottom: 16,
                        color: c.tone,
                        background: `color-mix(in srgb, ${c.tone} 13%, transparent)`,
                      }}
                    >
                      <Icon size={20} />
                    </span>
                    <h3 style={{ fontSize: 16, fontWeight: 700, letterSpacing: '-0.02em', margin: 0 }}>
                      {c.title}
                    </h3>
                    <p style={{ fontSize: 13.5, lineHeight: 1.6, color: 'var(--v-ink-2)', margin: '9px 0 0' }}>
                      {c.body}
                    </p>
                  </article>
                </Reveal>
              );
            })}
          </div>
        </div>
      </section>

      {/* --------------------------------------------------------- TRUST */}
      <section className="lp-section">
        <Reveal className="lp-panel">
          <span className="lp-eyebrow">What it will never do</span>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(230px, 1fr))',
              gap: 'clamp(20px, 3vw, 40px)',
              marginTop: 26,
            }}
          >
            {TRUST.map((t) => {
              const Icon = t.icon;
              return (
                <div key={t.title}>
                  <Icon size={19} style={{ color: 'var(--color-cortex-light)' }} />
                  <h3 style={{ fontSize: 15, fontWeight: 700, margin: '13px 0 0', letterSpacing: '-0.015em' }}>
                    {t.title}
                  </h3>
                  <p style={{ fontSize: 13, lineHeight: 1.6, color: 'var(--v-ink-2)', margin: '7px 0 0' }}>
                    {t.body}
                  </p>
                </div>
              );
            })}
          </div>
        </Reveal>
      </section>

      {/* ----------------------------------------------------------- CTA */}
      <section className="lp-section lp-cta-finale">
        <Reveal i={0}>
          <h2 className="lp-display" style={{ maxWidth: '16ch', margin: '0 auto' }}>
            Find out what your <span className="lp-gold">score</span> is.
          </h2>
        </Reveal>
        <Reveal i={1}>
          <p className="lp-lead" style={{ margin: '22px auto 0', textAlign: 'center' }}>
            Connect Gmail and Cortex Mail reads your last few hundred messages. Most
            people are surprised by the number.
          </p>
        </Reveal>
        <Reveal i={2}>
          <div className="lp-cta-row" style={{ justifyContent: 'center', marginTop: 30 }}>
            <button className="lp-btn lp-btn-primary" onClick={handleGoogleLogin}>
              Connect Gmail <ArrowRight size={17} />
            </button>
          </div>
        </Reveal>
      </section>

      <footer className="lp-footer">
        <div
          style={{
            maxWidth: 1180, margin: '0 auto',
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            gap: 16, flexWrap: 'wrap',
          }}
        >
          <BrandLogo
            size={28}
            textSize={13}
            showText
            onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
          />
          <span style={{ fontSize: 12, color: 'var(--v-ink-3)' }}>
            Navy-clear priorities · AES-256 at rest
          </span>
        </div>
      </footer>
    </div>
  );
};
