import React from 'react';
import { AppShell } from '../components/layout/AppShell';
import { Share2, Users } from 'lucide-react';

export const SharedPage: React.FC = () => {
  return (
    <AppShell title="Shared Emails" subtitle="Collaborate with your team">
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: 400,
        gap: 16,
        textAlign: 'center',
        animation: 'fadeIn 0.3s ease-out',
      }}>
        <div style={{
          width: 80,
          height: 80,
          borderRadius: 16,
          background: '#06b6d415',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          animation: 'scaleIn 0.4s ease-out',
        }}>
          <Share2 size={40} style={{ color: '#06b6d4' }} />
        </div>
        <h2 style={{ fontSize: 20, fontWeight: 700, margin: 0 }}>Shared Emails</h2>
        <p style={{ color: 'var(--text-2)', maxWidth: 400, margin: '8px 0 0' }}>
          Collaborate with your team members. Share emails, leave comments, and assign action items.
        </p>
        <div style={{
          marginTop: 24,
          padding: '16px 20px',
          borderRadius: 12,
          background: 'var(--surface-2)',
          display: 'flex',
          alignItems: 'center',
          gap: 12,
        }}>
          <Users size={20} style={{ color: 'var(--primary)' }} />
          <span style={{ fontSize: 14, color: 'var(--text-1)', fontWeight: 600 }}>
            Invite team members to collaborate
          </span>
        </div>
      </div>
    </AppShell>
  );
};
