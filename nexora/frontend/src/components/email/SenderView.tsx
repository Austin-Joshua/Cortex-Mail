import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { emailApi } from '../../api/emailApi';
import { queryKeys } from '../../api/queryKeys';
import type { SenderSummary } from '../../api/emailApi';
import { formatRelative } from '../../utils/formatDate';
import { User } from 'lucide-react';
import { useEmailStore } from '../../store/emailStore';

export const SenderView: React.FC = () => {
  const { setSelectedEmail } = useEmailStore();
  const [selectedSender, setSelectedSender] = React.useState<SenderSummary | null>(null);

  const { data: senders = [], isLoading: sendersLoading } = useQuery({
    queryKey: queryKeys.senders,
    queryFn: emailApi.getSenderSummary,
  });

  const { data: senderEmailsData, isLoading: emailsLoading } = useQuery({
    queryKey: queryKeys.senderEmails(selectedSender?.senderEmail),
    queryFn: () => emailApi.getEmailsFromSender(selectedSender!.senderEmail),
    enabled: !!selectedSender,
  });

  const senderEmails = senderEmailsData?.content ?? [];

  if (sendersLoading) {
    return (
      <div style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 8 }}>
        {[...Array(6)].map((_, i) => <div key={i} className="skeleton" style={{ height: 48 }} />)}
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', height: '100%', overflow: 'hidden', background: 'var(--bg)' }}>
      {/* Senders list */}
      <div style={{ width: selectedSender ? '40%' : '100%', borderRight: selectedSender ? '1px solid var(--border)' : 'none', overflowY: 'auto' }}>
        {senders.map((s) => {
          const isSelected = selectedSender?.senderEmail === s.senderEmail;
          return (
            <div
              key={s.senderEmail}
              onClick={() => setSelectedSender(s)}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                padding: '12px 16px',
                borderBottom: '1px solid var(--border)',
                background: isSelected ? 'var(--surface-2)' : 'transparent',
                cursor: 'pointer',
              }}
            >
              <div style={{ width: 32, height: 32, borderRadius: '50%', background: 'var(--accent-soft)', color: 'var(--accent)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700, fontSize: 13 }}>
                <User size={16} />
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, fontWeight: 700, color: 'var(--text-1)' }}>
                  <span>{s.senderName || s.senderEmail}</span>
                  <span style={{ fontSize: 11, color: 'var(--text-3)' }}>{s.emailCount} emails</span>
                </div>
                <p style={{ fontSize: 12, color: 'var(--text-2)', margin: '2px 0 0', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {s.latestSubject || '(no subject)'}
                </p>
              </div>
            </div>
          );
        })}
      </div>

      {/* Selected sender emails */}
      {selectedSender && (
        <div style={{ flex: 1, overflowY: 'auto', padding: 20 }}>
          <h2 style={{ fontSize: 18, fontWeight: 700, color: 'var(--text-1)', margin: '0 0 16px', fontFamily: 'Google Sans, Roboto, sans-serif' }}>
            Emails from {selectedSender.senderName || selectedSender.senderEmail}
          </h2>
          {emailsLoading ? (
            <div className="skeleton" style={{ height: 100 }} />
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {senderEmails.map((email) => (
                <div
                  key={email.id}
                  onClick={() => setSelectedEmail(email)}
                  style={{
                    padding: '12px 16px',
                    border: '1px solid var(--border)',
                    borderRadius: 8,
                    background: 'var(--surface)',
                    cursor: 'pointer',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, fontWeight: 700, color: 'var(--text-1)' }}>
                    <span>{email.subject}</span>
                    <span style={{ fontSize: 11, color: 'var(--text-3)' }}>{formatRelative(email.receivedAt)}</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
