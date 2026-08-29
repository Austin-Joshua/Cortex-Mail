import React, { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { emailApi } from '../../api/emailApi';
import axiosInstance from '../../api/axiosInstance';
import { CategoryTag } from '../common/CategoryTag';
import { PriorityBars } from '../common/PriorityBars';
import { formatDateTime } from '../../utils/formatDate';
import { cleanEmailBody } from '../../utils/cleanEmailBody';
import {
  CheckSquare, X, Calendar, Sparkles, ChevronDown, RefreshCw, MessageSquare
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useEmailStore } from '../../store/emailStore';

interface EmailDetailProps {
  emailId: number;
  onClose?: () => void;
}

export const EmailDetail: React.FC<EmailDetailProps> = ({ emailId, onClose }) => {
  const [showFullBody, setShowFullBody] = useState(false);
  const [showDraftReply, setShowDraftReply] = useState(false);
  const [draftStyle, setDraftStyle] = useState<'PROFESSIONAL' | 'FORMAL' | 'FRIENDLY' | 'CONCISE'>('PROFESSIONAL');
  const [draftText, setDraftText] = useState('');
  const [isDraftLoading, setIsDraftLoading] = useState(false);
  const [copied, setCopied] = useState(false);
  const [completingActions, setCompletingActions] = useState<Set<number>>(new Set());
  const [showThread, setShowThread] = useState(false);

  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { setSelectedEmail } = useEmailStore();

  const { data: email, isLoading } = useQuery({
    queryKey: ['email', emailId],
    queryFn: () => emailApi.getEmail(emailId),
  });

  React.useEffect(() => {
    if (email && !email.isRead) {
      emailApi.markAsRead(email.id)
        .then(() => {
          queryClient.invalidateQueries({ queryKey: ['emails'] });
          queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
        })
        .catch(() => {});
    }
  }, [email, email?.id, email?.isRead, queryClient]);

  const { data: threadEmails = [], isLoading: isThreadLoading } = useQuery({
    queryKey: ['email-thread', email?.gmailThreadId],
    queryFn: () => emailApi.getEmailThread(email!.gmailThreadId!),
    enabled: !!email?.gmailThreadId && showThread,
  });

  if (isLoading) {
    return (
      <div style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 16 }} className="animate-fade-in">
        {[...Array(4)].map((_, i) => (
          <div key={i} className="skeleton" style={{ height: i === 0 ? 36 : i === 1 ? 48 : 120 }} />
        ))}
      </div>
    );
  }

  if (!email) return null;

  const actionItems = (() => {
    if (email.actions && Array.isArray(email.actions)) {
      return email.actions.map((a: any) => ({
        id: a.id,
        description: a.actionDescription || a.description,
        deadline: a.deadline,
        action_type: a.actionType || a.action_type,
      }));
    }
    try {
      if (email.aiActionItems) {
        const parsed = typeof email.aiActionItems === 'string'
          ? JSON.parse(email.aiActionItems)
          : email.aiActionItems;
        if (Array.isArray(parsed)) {
          return parsed.map((a: any) => ({
            id: a.id,
            description: a.description || a.actionDescription,
            deadline: a.deadline,
            action_type: a.action_type || a.actionType,
          }));
        }
      }
    } catch {}
    return [];
  })();

  const rawBodyText = email.bodyFull || email.bodySnippet || '';
  const cleanedBody = cleanEmailBody(rawBodyText);
  const isLong = cleanedBody.length > 900;
  const displayBody = isLong && !showFullBody ? cleanedBody.slice(0, 900) + '...' : cleanedBody;
  const senderInitial = (email.senderName || email.senderEmail)[0]?.toUpperCase() ?? '?';

  const handleDraftReply = async () => {
    setIsDraftLoading(true);
    try {
      const { data } = await axiosInstance.post(`/api/emails/${emailId}/draft-reply`, { style: draftStyle });
      setDraftText(data.draft || '');
    } catch {
      setDraftText('Could not generate draft reply. Please try again.');
    } finally {
      setIsDraftLoading(false);
    }
  };

  const handleCopy = () => {
    navigator.clipboard.writeText(draftText);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleCompleteAction = async (actionId: number) => {
    if (!actionId || completingActions.has(actionId)) return;
    setCompletingActions(prev => new Set(prev).add(actionId));
    try {
      await axiosInstance.patch(`/api/email-actions/${actionId}/complete`);
      queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
      queryClient.invalidateQueries({ queryKey: ['email', emailId] });
    } finally {
      setCompletingActions(prev => {
        const s = new Set(prev);
        s.delete(actionId);
        return s;
      });
    }
  };

  return (
    <div
      style={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
        background: 'var(--bg)',
        padding: '24px 32px',
      }}
      className="animate-fade-in"
    >
      {/* Header section */}
      <div style={{ flexShrink: 0, marginBottom: 20 }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 16, marginBottom: 12 }}>
          <h1 style={{ fontSize: 20, fontWeight: 600, color: 'var(--text-1)', margin: 0, lineHeight: 1.3, flex: 1, fontFamily: 'Google Sans, Roboto, sans-serif' }}>
            {email.subject || '(no subject)'}
          </h1>
          {onClose && (
            <button
              onClick={onClose}
              style={{
                width: 32,
                height: 32,
                borderRadius: '50%',
                background: 'transparent',
                border: 'none',
                color: 'var(--text-2)',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <X size={18} />
            </button>
          )}
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
          <CategoryTag category={email.category} />
          <PriorityBars priority={email.priority as any} />
          {email.deadlineDetected && (
            <span style={{ fontSize: 12, color: 'var(--danger)', fontWeight: 600, display: 'inline-flex', alignItems: 'center', gap: 4 }}>
              <Calendar size={13} /> {formatDateTime(email.deadlineDetected)}
            </span>
          )}
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 14, paddingTop: 12, borderTop: '1px solid var(--border)' }}>
          <div
            style={{
              width: 40,
              height: 40,
              borderRadius: '50%',
              background: 'var(--accent)',
              color: '#ffffff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 16,
              fontWeight: 700,
              flexShrink: 0,
            }}
          >
            {senderInitial}
          </div>

          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span style={{ fontSize: 14, fontWeight: 700, color: 'var(--text-1)' }}>
                {email.senderName || 'Unknown Sender'}
              </span>
              <span style={{ fontSize: 12, color: 'var(--text-2)' }}>
                &lt;{email.senderEmail}&gt;
              </span>
            </div>
            <span style={{ fontSize: 12, color: 'var(--text-3)' }}>
              to me
            </span>
          </div>

          <div style={{ fontSize: 12, color: 'var(--text-2)' }}>
            {formatDateTime(email.receivedAt)}
          </div>
        </div>
      </div>

      {/* Main Content Area */}
      <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 20 }}>
        {/* Email Body */}
        <div
          style={{
            fontSize: 14,
            color: 'var(--text-1)',
            lineHeight: 1.6,
            whiteSpace: 'pre-wrap',
            fontFamily: 'Roboto, sans-serif',
          }}
        >
          {displayBody || '(No content)'}
          {isLong && (
            <button
              onClick={() => setShowFullBody(f => !f)}
              style={{
                marginTop: 12,
                display: 'block',
                fontSize: 13,
                color: 'var(--accent)',
                background: 'none',
                border: 'none',
                cursor: 'pointer',
                fontWeight: 600,
              }}
            >
              {showFullBody ? 'Show less' : 'Read full email'}
            </button>
          )}
        </div>

        {/* AI Summary Card */}
        {email.aiSummary && (
          <div className="surface-elevated" style={{ padding: 16 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8, color: 'var(--accent)' }}>
              <Sparkles size={16} />
              <span style={{ fontSize: 13, fontWeight: 700 }}>✨ AI Intelligence Summary</span>
            </div>
            <p style={{ fontSize: 13, color: 'var(--text-1)', margin: 0, lineHeight: 1.6 }}>
              {email.aiSummary}
            </p>
          </div>
        )}

        {/* Action Items Card */}
        {actionItems.length > 0 && (
          <div className="surface-elevated" style={{ padding: 16 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12, color: 'var(--accent)' }}>
              <CheckSquare size={16} />
              <span style={{ fontSize: 13, fontWeight: 700 }}>Action Items Required ({actionItems.length})</span>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {actionItems.map((a: any, i: number) => (
                <div
                  key={i}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 12,
                    padding: '8px 12px',
                    background: 'var(--bg)',
                    border: '1px solid var(--border)',
                    borderRadius: 6,
                  }}
                >
                  {a.id && (
                    <input
                      type="checkbox"
                      checked={completingActions.has(a.id)}
                      onChange={() => handleCompleteAction(a.id)}
                      style={{ cursor: 'pointer', accentColor: 'var(--accent)' }}
                    />
                  )}
                  <span style={{ flex: 1, fontSize: 13, color: 'var(--text-1)' }}>{a.description}</span>
                  {a.action_type && (
                    <span style={{ fontSize: 11, fontWeight: 700, color: 'var(--accent)', background: 'var(--accent-soft)', padding: '2px 6px', borderRadius: 4 }}>
                      {a.action_type}
                    </span>
                  )}
                </div>
              ))}
            </div>

            <div style={{ display: 'flex', gap: 10, marginTop: 12 }}>
              <button
                className="btn-outline"
                onClick={() => navigate(`/brain?context=email:${emailId}`)}
              >
                <Sparkles size={14} /> Ask Cortex Brain
              </button>
              <button
                className="btn-outline"
                onClick={() => {
                  setShowDraftReply(d => !d);
                  if (!showDraftReply && !draftText) handleDraftReply();
                }}
              >
                <MessageSquare size={14} /> Draft Reply
              </button>
            </div>
          </div>
        )}

        {/* Draft Reply Card */}
        {showDraftReply && (
          <div className="surface-elevated" style={{ padding: 16 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12, color: 'var(--accent)' }}>
              <MessageSquare size={16} />
              <span style={{ fontSize: 13, fontWeight: 700 }}>↩ Draft AI Reply</span>
            </div>

            <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
              {(['PROFESSIONAL', 'FORMAL', 'FRIENDLY', 'CONCISE'] as const).map(s => (
                <button
                  key={s}
                  onClick={() => setDraftStyle(s)}
                  style={{
                    padding: '4px 12px',
                    borderRadius: 16,
                    fontSize: 12,
                    border: '1px solid var(--border)',
                    background: draftStyle === s ? 'var(--accent-soft)' : 'var(--bg)',
                    color: draftStyle === s ? 'var(--accent)' : 'var(--text-1)',
                    fontWeight: draftStyle === s ? 700 : 400,
                    cursor: 'pointer',
                  }}
                >
                  {s}
                </button>
              ))}
            </div>

            {isDraftLoading ? (
              <div style={{ padding: 16, color: 'var(--text-2)', fontSize: 13, display: 'flex', alignItems: 'center', gap: 8 }}>
                <RefreshCw size={14} className="animate-spin" /> Generating reply with Gemini AI...
              </div>
            ) : (
              <textarea
                value={draftText}
                onChange={e => setDraftText(e.target.value)}
                style={{
                  width: '100%',
                  minHeight: 110,
                  background: 'var(--bg)',
                  border: '1px solid var(--border)',
                  borderRadius: 6,
                  padding: 12,
                  fontSize: 13,
                  color: 'var(--text-1)',
                  outline: 'none',
                  fontFamily: 'Roboto, sans-serif',
                }}
              />
            )}

            <div style={{ display: 'flex', gap: 8, marginTop: 10 }}>
              <button className="btn-accent" onClick={handleCopy}>
                {copied ? 'Copied!' : 'Copy to Clipboard'}
              </button>
              <button className="btn-outline" onClick={handleDraftReply} disabled={isDraftLoading}>
                Regenerate
              </button>
            </div>
          </div>
        )}

        {/* Thread Card */}
        {email.gmailThreadId && (
          <div className="surface-elevated" style={{ padding: 16 }}>
            <button
              onClick={() => setShowThread(t => !t)}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 6,
                fontSize: 13,
                fontWeight: 700,
                color: 'var(--accent)',
                background: 'none',
                border: 'none',
                cursor: 'pointer',
                padding: 0,
              }}
            >
              <ChevronDown
                size={16}
                style={{ transform: showThread ? 'rotate(180deg)' : 'none', transition: 'transform 0.2s' }}
              />
              🧵 Email Thread ({threadEmails.length || 'view messages'})
            </button>

            {showThread && (
              <div style={{ marginTop: 12, display: 'flex', flexDirection: 'column', gap: 8 }}>
                {isThreadLoading ? (
                  <div className="skeleton" style={{ height: 48 }} />
                ) : (
                  threadEmails.map((sibling: any) => (
                    <div
                      key={sibling.id}
                      onClick={() => setSelectedEmail(sibling)}
                      style={{
                        padding: '10px 12px',
                        background: sibling.id === emailId ? 'var(--surface-2)' : 'var(--bg)',
                        border: '1px solid var(--border)',
                        borderRadius: 6,
                        cursor: 'pointer',
                      }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, fontWeight: 700, color: 'var(--text-1)' }}>
                        <span>{sibling.senderName || sibling.senderEmail}</span>
                        <span style={{ color: 'var(--text-3)' }}>{formatDateTime(sibling.receivedAt)}</span>
                      </div>
                      <p style={{ fontSize: 12, color: 'var(--text-2)', margin: '4px 0 0', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {sibling.bodySnippet}
                      </p>
                    </div>
                  ))
                )}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
