import React from 'react';
import type { BrainMessage } from '../../types/Brain';
import { Brain, User as UserIcon, ExternalLink } from 'lucide-react';
import { formatRelative } from '../../utils/formatDate';

interface Props {
  message: BrainMessage;
  onEmailClick: (id: number) => void;
}

export const BrainMessageComponent: React.FC<Props> = ({ message, onEmailClick }) => {
  const isUser = message.type === 'user';

  return (
    <div style={{ display: 'flex', gap: 12, flexDirection: isUser ? 'row-reverse' : 'row' }}>
      <div
        style={{
          width: 32,
          height: 32,
          borderRadius: '50%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          flexShrink: 0,
          background: isUser ? 'var(--color-surface-active)' : 'var(--color-cortex-soft)',
          border: `1px solid ${isUser ? 'var(--color-border)' : 'var(--color-cortex-border)'}`,
          color: isUser ? 'var(--color-text-primary)' : 'var(--color-cortex-light)',
        }}
      >
        {isUser ? <UserIcon size={16} /> : <Brain size={16} />}
      </div>

      <div style={{ maxWidth: '80%', display: 'flex', flexDirection: 'column', gap: 6, alignItems: isUser ? 'flex-end' : 'flex-start' }}>
        <span className="section-label" style={{ fontSize: 10 }}>
          {isUser ? 'You' : 'Brain'}
        </span>

        <div className={isUser ? 'brain-user-bubble' : 'brain-ai-bubble'}>
          {message.content}
        </div>

        {!isUser && message.referencedEmails && message.referencedEmails.length > 0 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6, width: '100%', marginTop: 4 }}>
            <span className="section-label" style={{ fontSize: 10 }}>SOURCE EMAILS ({message.referencedEmails.length})</span>
            {message.referencedEmails.map((email) => (
              <button
                key={email.id}
                type="button"
                onClick={() => onEmailClick(email.id)}
                className="brain-source-link"
              >
                <div style={{ flex: 1, minWidth: 0 }}>
                  <p style={{ fontSize: 12, fontWeight: 600, color: 'var(--color-info)', margin: '0 0 2px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {email.subject || '(no subject)'}
                  </p>
                  <p style={{ fontSize: 11, color: 'var(--color-text-muted)', margin: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {email.senderName || email.senderEmail}
                  </p>
                </div>
                <ExternalLink size={12} style={{ color: 'var(--color-text-muted)' }} />
              </button>
            ))}
          </div>
        )}

        <span style={{ fontSize: 10, color: 'var(--color-text-muted)' }}>
          {formatRelative(message.timestamp.toISOString())}
        </span>
      </div>
    </div>
  );
};
