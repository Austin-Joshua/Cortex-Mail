import React from 'react';
import { AppShell } from '../components/layout/AppShell';
import { Clock } from 'lucide-react';

export const ScheduledEmailsPage: React.FC = () => {
  return (
    <AppShell title="Scheduled Emails" subtitle="Emails queued for later">
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: 400,
        gap: 16,
        textAlign: 'center',
      }}>
        <div style={{
          width: 80,
          height: 80,
          borderRadius: 16,
          background: '#00d4aa15',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}>
          <Clock size={40} style={{ color: '#00d4aa' }} />
        </div>
        <h2 style={{ fontSize: 20, fontWeight: 700, margin: 0 }}>Scheduled Emails</h2>
        <p style={{ color: 'var(--text-2)', maxWidth: 400 }}>
          Schedule emails to send at the perfect time with AI-optimized delivery suggestions.
        </p>
      </div>
    </AppShell>
  );
};
