import React from 'react';
import { AppShell } from '../components/layout/AppShell';
import { FileText } from 'lucide-react';

export const DraftsPage: React.FC = () => {
  return (
    <AppShell title="Drafts" subtitle="Your unsent email drafts">
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
          background: '#3b4fea15',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}>
          <FileText size={40} style={{ color: '#3b4fea' }} />
        </div>
        <h2 style={{ fontSize: 20, fontWeight: 700, margin: 0 }}>Drafts</h2>
        <p style={{ color: 'var(--text-2)', maxWidth: 400 }}>
          Save and manage your email drafts with AI suggestions and smart scheduling.
        </p>
      </div>
    </AppShell>
  );
};
