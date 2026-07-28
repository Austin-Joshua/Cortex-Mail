import React from 'react';
import { AppShell } from '../components/layout/AppShell';
import { Archive } from 'lucide-react';

export const ArchivePage: React.FC = () => {
  return (
    <AppShell title="Archive" subtitle="Stored emails and past messages">
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
          background: '#64748b15',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          animation: 'scaleIn 0.4s ease-out',
        }}>
          <Archive size={40} style={{ color: '#64748b' }} />
        </div>
        <h2 style={{ fontSize: 20, fontWeight: 700, margin: 0 }}>Archive</h2>
        <p style={{ color: 'var(--text-2)', maxWidth: 400, margin: '8px 0 0' }}>
          Your archived emails are stored here. Search and retrieve past messages with advanced filtering.
        </p>
      </div>
    </AppShell>
  );
};
