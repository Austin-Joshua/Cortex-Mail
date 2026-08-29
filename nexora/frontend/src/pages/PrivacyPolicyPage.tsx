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
              background: 'var(--ember-soft)',
              color: 'var(--ember)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Shield size={20} />
          </div>
          <div>
            <h1 style={{ fontSize: 24, fontWeight: 800, margin: 0, fontFamily: 'Google Sans, Roboto, sans-serif' }}>
              Privacy Policy &amp; OAuth Disclosure
            </h1>
            <p style={{ fontSize: 13, color: 'var(--text-2)', margin: '2px 0 0' }}>
              Last updated: July 2026
            </p>
          </div>
        </div>

        <div className="card-paper" style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 20 }}>
          <section>
            <h2 style={{ fontSize: 16, fontWeight: 700, color: 'var(--ember)', margin: '0 0 8px' }}>
              1. Gmail OAuth Access &amp; Read Scope
            </h2>
            <p style={{ fontSize: 14, color: 'var(--text-2)', lineHeight: 1.6, margin: 0 }}>
              Cortex Mail requests Google OAuth access to fetch and analyze incoming emails for deadlines, priority classification, and AI summaries. We request read-only access by default and optional send permissions for user-initiated email replies.
            </p>
          </section>

          <section>
            <h2 style={{ fontSize: 16, fontWeight: 700, color: 'var(--ember)', margin: '0 0 8px' }}>
              2. Data Protection &amp; Encryption
            </h2>
            <p style={{ fontSize: 14, color: 'var(--text-2)', lineHeight: 1.6, margin: 0 }}>
              Your OAuth access tokens are encrypted at rest using industry-standard AES-256 encryption. Raw email content is analyzed securely and stored only as metadata (summaries, action items, deadlines) required for your dashboard.
            </p>
          </section>

          <section>
            <h2 style={{ fontSize: 16, fontWeight: 700, color: 'var(--ember)', margin: '0 0 8px' }}>
              3. Data Sharing &amp; Privacy Guarantee
            </h2>
            <p style={{ fontSize: 14, color: 'var(--text-2)', lineHeight: 1.6, margin: 0 }}>
              Cortex Mail never sells, rents, or shares your personal email data with third-party advertisers. All email classification is powered by isolated AI models operating under strict privacy constraints.
            </p>
          </section>
        </div>
      </div>
    </div>
  );
};
