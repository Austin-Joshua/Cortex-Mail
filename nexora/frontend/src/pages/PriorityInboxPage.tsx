import React from 'react';
import { AppShell } from '../components/layout/AppShell';
import { Zap } from 'lucide-react';

export const PriorityInboxPage: React.FC = () => {
  return (
    <AppShell title="Priority Inbox" subtitle="AI-learned important emails">
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
          background: '#ff6b3515',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}>
          <Zap size={40} style={{ color: '#ff6b35' }} />
        </div>
        <h2 style={{ fontSize: 20, fontWeight: 700, margin: 0 }}>Priority Inbox</h2>
        <p style={{ color: 'var(--text-2)', maxWidth: 400 }}>
          Your most important emails, intelligently prioritized by Nexora AI based on your patterns and preferences.
        </p>
      </div>
    </AppShell>
  );
};
