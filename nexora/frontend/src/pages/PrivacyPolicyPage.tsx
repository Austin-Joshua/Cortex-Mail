import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Shield, ArrowLeft } from 'lucide-react';

export const PrivacyPolicyPage: React.FC = () => {
  const navigate = useNavigate();

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg)', color: 'var(--text-1)', padding: '40px 24px' }}>
      <div style={{ maxWidth: 800, margin: '0 auto' }}>
        <button
          onClick={() => navigate(-1)}
          className="btn-outline"
          style={{ marginBottom: 24, fontSize: 13 }}
        >
          <ArrowLeft size={16} /> Back
        </button>

        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
          <div
            style={{
              width: 40,
              height: 40,
              borderRadius: 10,
              background: 'var(--v-navy-soft)',
              color: 'var(--v-navy)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Shield size={20} />
          </div>
          <div>
            <h1 style={{ fontSize: 24, fontWeight: 800, margin: 0 }}>
              Privacy Policy &amp; OAuth Disclosure
            </h1>
            <p style={{ fontSize: 13, color: 'var(--text-2)', margin: '2px 0 0' }}>
              Last updated: August 2026 · Cortex Mail
            </p>
          </div>
        </div>

        <div className="card-paper" style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 20 }}>
          <section>
            <h2 style={{ fontSize: 16, fontWeight: 700, color: 'var(--v-navy)', margin: '0 0 8px' }}>
              1. Gmail OAuth access
            </h2>
            <p style={{ fontSize: 14, color: 'var(--text-2)', lineHeight: 1.6, margin: 0 }}>
              Cortex Mail requests Google OAuth access to sync and analyze your Gmail for classification,
              Cortex Score, deadlines, and Brain Q&amp;A. When you use in-app controls, we may also apply
              Gmail mailbox actions you initiate (mark read/unread, star, archive, trash/restore).
              We do not send email on your behalf unless a future feature explicitly asks you to confirm a send.
            </p>
          </section>

          <section>
            <h2 style={{ fontSize: 16, fontWeight: 700, color: 'var(--v-navy)', margin: '0 0 8px' }}>
              2. Data protection &amp; encryption
            </h2>
            <p style={{ fontSize: 14, color: 'var(--text-2)', lineHeight: 1.6, margin: 0 }}>
              OAuth tokens are encrypted at rest (AES-256). Synced mail content is stored in your
              application database (Supabase Postgres in production) and scoped to your account.
            </p>
          </section>

          <section>
            <h2 style={{ fontSize: 16, fontWeight: 700, color: 'var(--v-navy)', margin: '0 0 8px' }}>
              3. Data sharing
            </h2>
            <p style={{ fontSize: 14, color: 'var(--text-2)', lineHeight: 1.6, margin: 0 }}>
              Cortex Mail does not sell or rent your email content for advertising. Optional AI providers
              (Gemini/Claude) may receive excerpts only when those features are enabled and used.
            </p>
          </section>

          <section>
            <h2 style={{ fontSize: 16, fontWeight: 700, color: 'var(--v-navy)', margin: '0 0 8px' }}>
              4. Google API Services User Data Policy
            </h2>
            <p style={{ fontSize: 14, color: 'var(--text-2)', lineHeight: 1.6, margin: 0 }}>
              Use of information from Google APIs complies with the{' '}
              <a href="https://developers.google.com/terms/api-services-user-data-policy" target="_blank" rel="noopener noreferrer">
                Google API Services User Data Policy
              </a>
              , including Limited Use.
            </p>
          </section>
        </div>
      </div>
    </div>
  );
};
