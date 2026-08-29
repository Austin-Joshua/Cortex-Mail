import React from 'react';
import { EmailCard } from './EmailCard';
import type { Email } from '../../types/Email';
import { useEmailStore } from '../../store/emailStore';
import { Inbox } from 'lucide-react';

interface EmailListProps {
  emails: Email[];
  isLoading: boolean;
  onEmailSelect?: (email: Email) => void;
}

export const EmailList: React.FC<EmailListProps> = ({ emails, isLoading, onEmailSelect }) => {
  const { selectedEmail } = useEmailStore();

  if (isLoading) {
    return (
      <div style={{ background: 'var(--bg)' }}>
        {Array.from({ length: 10 }).map((_, i) => (
          <div
            key={i}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 16,
              height: 44,
              padding: '0 16px',
              borderBottom: '1px solid var(--border)',
            }}
          >
            <div className="skeleton" style={{ width: 16, height: 16, borderRadius: 2 }} />
            <div className="skeleton" style={{ width: 18, height: 18, borderRadius: 2 }} />
            <div className="skeleton" style={{ width: 140, height: 14, borderRadius: 4 }} />
            <div className="skeleton" style={{ flex: 1, height: 14, borderRadius: 4 }} />
            <div className="skeleton" style={{ width: 64, height: 14, borderRadius: 4 }} />
          </div>
        ))}
      </div>
    );
  }

  if (emails.length === 0) {
    return (
      <div
        className="animate-fade-in"
        style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '64px 24px',
          textAlign: 'center',
          background: 'var(--bg)',
        }}
      >
        <div
          style={{
            width: 56,
            height: 56,
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            marginBottom: 16,
            background: 'var(--surface)',
            border: '1px solid var(--border)',
          }}
        >
          <Inbox size={24} style={{ color: 'var(--text-3)' }} />
        </div>
        <p style={{ color: 'var(--text-1)', fontWeight: 700, fontSize: 14, margin: '0 0 4px', fontFamily: 'Google Sans, Roboto, sans-serif' }}>
          Your inbox is clear
        </p>
        <p style={{ color: 'var(--text-2)', fontSize: 13, margin: 0 }}>
          Sync from the title bar if your inbox looks empty.
        </p>
      </div>
    );
  }

  return (
    <div style={{ background: 'var(--bg)' }}>
      {emails.map((email) => (
        <EmailCard
          key={email.id}
          email={email}
          isSelected={selectedEmail?.id === email.id}
          onClick={() => onEmailSelect?.(email)}
        />
      ))}
    </div>
  );
};
